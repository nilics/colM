package me.xuxiaoxiao.multinetwork;

import com.google.gson.Gson;
import me.xuxiaoxiao.multinetwork.base.ClientThread;
import me.xuxiaoxiao.multinetwork.base.ServerThread;
import me.xuxiaoxiao.multinetwork.msg.*;
import me.xuxiaoxiao.xtools.common.XTools;
import me.xuxiaoxiao.xtools.common.config.XConfigTools;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.spec.DHParameterSpec;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.util.*;

public class Handler {
    public static final Gson GSON = new Gson();

    public static final String CFG_PREFIX = "me.xuxiaoxiao$multinetwork$";
    public static final String CFG_SERVER_PORT = CFG_PREFIX + "server.port";

    //加密需要：非对称密钥算法
    public static final String KEY_ALGORITHM = "ElGamal";
    /**
     * 密钥长度，DH算法的默认密钥长度是1024
     * 密钥长度必须是8的倍数，在160到16384位之间
     */
    private static final int KEY_SIZE = 256;
    private static final String PUBLIC_KEY = "ElGamalPublicKey"; //公钥
    private static final String PRIVATE_KEY = "ElGamalPrivateKey"; //私钥
    private static boolean True=true;

    private static BigInteger p;
    private static BigInteger g;
    private static Integer k=2;
    private static Integer nodesNum=1000;

    public static void main(String[] args) throws Exception {
        XConfigTools.X_CONFIGS.cfgLoad("config.properties", "utf-8");

        Scanner scanner = new Scanner(System.in);
        //System.out.println("请输入服务端ID：使用 IP:端口");//自己的端口号：localhost, 端口：8000
        ServerThread serverThread = new ServerThread(args[0]);
        serverThread.start();

        //  System.out.println("请输入要连接的 IP:端口，直接按回车键跳过");
        String address = args[1];
        if (!XTools.strBlank(address)) {//别人的，要连接的地址，XTools.strBlank，判断地址是否为空
            serverThread.connect(address); //建立连接
        }

        HashMap<Integer, HashSet<Integer>> G = new HashMap<Integer, HashSet<Integer>>();

        boolean admin = Boolean.parseBoolean(args[2]);//是否是管理机器
        System.out.println("start handle");
        String filename=null;
        String writeFilename=null;
        if (serverThread.handlerId.equals("localhost:8000")){
            filename="E:\\javaWorkspace\\MultiNetwork-zhenghe\\src\\main\\java\\me\\xuxiaoxiao\\multinetwork\\data\\G1.txt";
            writeFilename="E:\\javaWorkspace\\MultiNetwork-zhenghe\\src\\main\\java\\me\\xuxiaoxiao\\multinetwork\\data\\G1Communities.txt";

            System.out.println("read G1");
        }else if(serverThread.handlerId.equals("localhost:8001")){
            filename="E:\\javaWorkspace\\MultiNetwork-zhenghe\\src\\main\\java\\me\\xuxiaoxiao\\multinetwork\\data\\G2.txt";
            writeFilename="E:\\javaWorkspace\\MultiNetwork-zhenghe\\src\\main\\java\\me\\xuxiaoxiao\\multinetwork\\data\\G2Communities.txt";

            System.out.println("read G2");
        }else if (serverThread.handlerId.equals("localhost:8002")){
            filename="E:\\javaWorkspace\\MultiNetwork-zhenghe\\src\\main\\java\\me\\xuxiaoxiao\\multinetwork\\data\\G3.txt";
            writeFilename="E:\\javaWorkspace\\MultiNetwork-zhenghe\\src\\main\\java\\me\\xuxiaoxiao\\multinetwork\\data\\G3Communities.txt";

            System.out.println("read G3");
        }
        serialize(filename,G);
        //conGragh(filename,0.1, 0.2,0.1,0);
        ArrayList<Integer> givenNodeList=new ArrayList<>();
        //givenNodeList.add( 0);
        givenNodeList.add(1);
        givenNodeList.add(2);
        //Integer givennode = 0;要不要块状计算，避免多次通信
        for (Integer givennode:givenNodeList){
            System.out.println("start givennode"+givennode);
            HashSet<Integer> k_neighbors = (HashSet<Integer>) G.get(givennode).clone();
            HashSet<Integer> tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
            for (int j = 1; j < k; j++) {
                for (Integer node : tempneighbors) {
                    k_neighbors.addAll(G.get(node));
                }
                tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                System.out.println("tempneighbors"+tempneighbors);
            }

            if (admin) {
                while (true) {  //广播
                    Thread.sleep(1000);
                    if (serverThread.clients().size() >= 3) { //有3个节点开始处理
                        //serverThread.broadcast(new EdgeReq(serverThread.getHandlerId(), "fff"));·
                        ElGamalSafeUnionAdminNetwork(tempneighbors,nodesNum,serverThread);

                        colMAdmin (givennode,serverThread,G, writeFilename);
                        //serverThread.broadcast(new EdgeReq(serverThread.getHandlerId(), "fff"));
                        break;
                    }
                }
            }
            else{
                //接收来自admin网络的p,g
                ElGamalSafeUnionSubNetwork(tempneighbors, nodesNum, serverThread);
                colMsub (givennode,serverThread,G);
                //break;

            }
            System.out.println("finish one node");
        }
        serverThread.broadcast(new End(-1));//代码运行完,flag置为-1
        System.out.println("发送end消息");
        System.exit(0);
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
    }
    //加密算法
    public static Miwen encrypt(BigInteger publickey, BigInteger M, BigInteger p, BigInteger g){
        try {

            StringBuffer C=new StringBuffer();
            Random ran=new  Random();
            //k<p-1.我们这里取他的子集
            int k=ran.nextInt(1000000000);
            //计算c1
            BigInteger C1=g.modPow(new BigInteger(k+""), p);
            //计算c2
            BigInteger C2=( publickey).modPow(new BigInteger(k+""), p).multiply(M).mod(p);

            C.append(C1+":"+C2+",");
            Miwen temps=new Miwen(C1,  C2);
            return temps;
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        return null;
    }

    public static  void ElGamalSafeUnionSubNetwork( HashSet<Integer> tempneighbors, Integer nodesNum,ServerThread serverThread) throws Exception {
        //接收p和g；
        BigInteger p=new BigInteger("0");
        BigInteger g=new BigInteger("0");
        System.out.println("received pandg start");
        while (True){
            if (serverThread.pandg.p.equals(new BigInteger(String.valueOf(0)))){
                Thread.sleep(1000);
            }else{
                p = serverThread.pandg.p;
                g = serverThread.pandg.g;
                break;
            }
        }
        System.out.println("received pandg end");
        //3. 生成自己的私钥xi
        StringBuffer key=new StringBuffer();
        //随机生成256位的密钥
        for(int i=0;i<KEY_SIZE;i++) {
            Random ran=new Random();
            int x=ran.nextInt(10);
            if(x>5)
                key=key.append(1);
            else
                key=key.append(0);
        }
        //构造私钥xi
        BigInteger privateKey=new BigInteger(key.toString(),2);

        //4. 计算hi (publicKey)，并发送给主网络 hi=g^xi mod p
        Hi hi = new Hi( g.modPow(privateKey, p));
        serverThread.handlers.get("localhost:8000").send(hi);
        //System.out.println("send  hi-2"+hi.hi.toString());

        Hi h=new Hi(new BigInteger("0")); //7. 接收来自主网络的公钥h。
        while (True){
            if (serverThread.his.size()!=0){
                synchronized (serverThread.his){
                    h=  serverThread.his.get(0);
                }
                break;
            }else{
                Thread.sleep(1000);
            }
        }
        UV uvi = new UV(new ArrayList<BigInteger>(),new ArrayList<BigInteger>());//对节点进行1-r编码,并对值进行加密。
        //将网络中的节点编码为数组，如果是图中的节点，则对应的位置是大于1的随机数；如果不是图中的节点，则数组中元素为1；
        for (Integer i = 0; i < nodesNum; i++) {
            if (tempneighbors.contains(i)) {
                Random ran=new Random();
                Miwen temp=encrypt( h.hi, new BigInteger(String.valueOf(ran.nextInt(10)+2)), p, g);
                uvi.u.add(temp.u);
                uvi.v.add(temp.v);
            } else {
                Miwen  temp=encrypt( h.hi, new BigInteger("1"), p, g);
                uvi.u.add(temp.u);
                uvi.v.add(temp.v);
            }
        }

        //10. 从前一个客户端接收数据，乘上当前的值，
        UV uv=new UV(new ArrayList(serverThread.uv.u),new ArrayList(serverThread.uv.v));
        //System.out.println("received uv"+uv);
        while (True){
            if (uv.u.size()!=0){
                serverThread.uv=new UV(new ArrayList(),new ArrayList()); //清空serverThread.uv;
                break;
            }else{
                Thread.sleep(1000);
                synchronized (serverThread.uv){
                    uv= new UV(new ArrayList(serverThread.uv.u),new ArrayList(serverThread.uv.v));
                }
            }
        }
        for (int i = 0; i < nodesNum; i++) {
            uv.u.set(i, uv.u.get(i).multiply(uvi.u.get(i))); //c相乘
            uv.v.set(i,uv.v.get(i).multiply(uvi.v.get(i))); //v相乘
        }

         //调试看一下，这个语句执行完以后，uv会不会已经被清空。
        //将uv传输给下一个客户端
        System.out.println("serverThread.handlerId:"+serverThread.handlerId);
        if (serverThread.handlerId.equals("localhost:8001")){
            serverThread.handlers.get("localhost:8002").send(uv);
        }else if (serverThread.handlerId.equals("localhost:8002")){
            serverThread.handlers.get("localhost:8000").send(uv);
            //serverThread.uv=new UV(new ArrayList(),new ArrayList()); //清空serverThread.uv;
        }
        System.out.println("waiting"+uv);
        //12 接收密文u，计算wi，wi=u^xi  mod p
        UV uvt = new UV(new ArrayList<BigInteger>(),new ArrayList<BigInteger>());
        while (True){
            synchronized (serverThread.uv){
                uvt= serverThread.uv; //uv会不会是之前的旧值。
            }
            if (uvt.u.size()!=0){
                break;
            }else{
                Thread.sleep(1000);
            }
        }
        W wi = new W(new ArrayList<BigInteger>());
        for (int i = 0; i < nodesNum; i++) {
            wi.w.add( uvt.u.get(i).modPow(privateKey, p));
        }
        serverThread.handlers.get("localhost:8000").send(wi);//并发送wi给主网络，

          //清空，以供下次循环使用
        synchronized (serverThread.uv){serverThread.uv.u.clear();serverThread.uv.v.clear();}
        synchronized (serverThread.his){ serverThread.his.clear();}
        synchronized(serverThread.ws){ serverThread.ws.clear();}
        synchronized(serverThread.pandg){serverThread.pandg=new PrimeGen(new BigInteger(String.valueOf(0)),new BigInteger(String.valueOf(0)));}

        //等待协调节点发送并集节点
        while (True){
            System.out.println("serverThread.nodes.size()，waiting"+serverThread.nodes.size());
            if (serverThread.nodes.size()>=1){
                serverThread.candidateNodes=(HashSet<Integer>) serverThread.nodes.clone();;
                break;
            }else{
                Thread.sleep(100);
            }
        }

    }

    public static void ElGamalSafeUnionAdminNetwork(HashSet<Integer> tempneighbors, Integer nodesNum, ServerThread serverThread) throws Exception {

        Security.addProvider(new BouncyCastleProvider()); //加入对BouncyCastle支持
        AlgorithmParameterGenerator apg = AlgorithmParameterGenerator.getInstance(KEY_ALGORITHM);
        apg.init(KEY_SIZE);//初始化参数生成器
        AlgorithmParameters params = apg.generateParameters();//生成算法参数
        //构建参数材料
        DHParameterSpec elParams = (DHParameterSpec) params.getParameterSpec(DHParameterSpec.class);
        BigInteger p = elParams.getP();
        BigInteger g = elParams.getG();

//        BigInteger p=new BigInteger("23333549391663695385288013284442107401803411405323301196491683562780320236703944716560126054468785184158255912186304412517433339996290968387400967503917967");
//        BigInteger g = new BigInteger("5");
        PrimeGen primeGen=new PrimeGen(p,g);
        serverThread.broadcast(primeGen); //发送 p, g

        System.out.println("broadcast p and g");
        //3. 生成自己的私钥xi
        StringBuffer key=new StringBuffer(); //随机生成256位的密钥
        for(int i=0;i<KEY_SIZE;i++) {
            Random ran=new Random();
            int x=ran.nextInt(10);
            if(x>5)
                key=key.append(1);
            else
                key=key.append(0);
        }
        //构造私钥xi
        BigInteger privateKey=new BigInteger(key.toString(),2);
        //privateKey=new BigInteger("104147137590674773303301157249396851417376887624771913623292646091799108959712");
        BigInteger hi =  g.modPow( privateKey, p);  //当前网络的计算hi (publicKey)=g^xi mod p

        System.out.println("received hi-self"+hi.toString());

        ArrayList<Hi> temps=new ArrayList<>();
        //7. 接收来自子网络的hi，计算公钥h，
        //System.out.println("received hi");
        while (True){
            synchronized (serverThread.his) {
                temps=serverThread.his;
            }
            if (temps.size()>=(serverThread.handlers.size()-1)){
                break;
            }else{
                Thread.sleep(1000);
            }
        }
        System.out.println("received hi-ok");

        Hi h=  new Hi(hi);
        for (Hi temp:temps){
            h.hi=h.hi.multiply(temp.hi).mod(p);
        }
        h.hi=h.hi.mod(p);
        serverThread.broadcast(h); //发送给所有的自网络。
        System.out.println("serverThread.his---要加锁，因为要清空");
        synchronized (serverThread.his){serverThread.his.clear();} //加锁，防止其他进程更改



        UV uvi = new UV(new ArrayList<BigInteger>(),new ArrayList<BigInteger>());//对节点进行1-r编码,并对值进行加密。
        //将网络中的节点编码为数组，如果是图中的节点，则对应的位置是大于1的随机数；如果不是图中的节点，则数组中元素为1；
        for (Integer i = 0; i < nodesNum; i++) {
            if (tempneighbors.contains(i)) {
                Random ran=new Random();
                Miwen temp=encrypt( h.hi, new BigInteger(String.valueOf(ran.nextInt(10)+2)), p, g);
                uvi.u.add(temp.u);
                uvi.v.add(temp.v);
            } else {
                Miwen  temp=encrypt( h.hi, new BigInteger("1"), p, g);
                uvi.u.add(temp.u);
                uvi.v.add(temp.v);
            }
        }
        System.out.println("send_mapingcode");
        //9. 加密后的信息传输给下一个客户端
        serverThread.handlers.get("localhost:8001").send(uvi);

        //11. 接收来自最后一个客户端发来的消息(v,u)，并将收到的密文u进行广播
        UV uv;
        synchronized (serverThread.uv){
        uv= serverThread.uv;
        }
        while (True){
            if (uv.u.size()!=0){
                break;
            }else{
                Thread.sleep(1000);
                synchronized (serverThread.uv){
                    uv= serverThread.uv;
                }
            }
        }

        serverThread.broadcast(uv); //收到的密文u进行广播
        W wi = new W(new ArrayList<BigInteger>());//12 根据密文u，计算wi，wi=u^xi  mod p
        for (int i = 0; i < nodesNum; i++) {
            wi.w.add(uv.u.get(i).modPow(privateKey, p));
        }

        //13 接收来自各个子网络的wi，即w1和w2;根据u，v,w解密，
        ArrayList<W> wis=(ArrayList<W>)(serverThread.ws.clone());

        while (True){
            if (wis.size()>=(serverThread.handlers.size()-1)){
                break;
            }else{
                Thread.sleep(1000);
                synchronized (serverThread.ws) {
                    wis=serverThread.ws;
                }
            }
        }
        ArrayList<BigInteger> w1 =wis.get(0).w;
        ArrayList<BigInteger> w2 =wis.get(1).w;

        BigInteger[] a = new BigInteger[nodesNum];
        for (int t = 0; t < nodesNum; t++) {
            wi.w.set(t,wi.w.get(t).multiply(w1.get(t)).multiply(w2.get(t)));
            //a[t] = encrypt(miwen, "privateKey");   //v*W^-1 mod p; p是已知的，v是密文，能不能设置参数，然后用这个函数（可以，密文就是两个数，C1，C2）。涉及到计算逆元，比较麻烦。
            a[t]= uv.v.get(t).multiply(wi.w.get(t).modInverse(p)).mod(p);
            if (a[t].equals(new BigInteger("1"))==false){ //获得并集
                //System.out.println("t"+t);
                serverThread.nodes.add(t);
            }
        }
        //清空给使用的变量
        serverThread.his.clear();
        serverThread.ws.clear();
        serverThread.uv.u.clear();
        serverThread.uv.v.clear();
        serverThread.pandg=new PrimeGen(new BigInteger(String.valueOf(0)),new BigInteger(String.valueOf(0)));

        //获得并集并广播（求所有网络的k_neighbors的并集）
        serverThread.candidateNodes=(HashSet<Integer>) serverThread.nodes.clone();
        serverThread.broadcast(serverThread.nodes);
    }

    private static void serialize(String filename,HashMap<Integer, HashSet<Integer>> G) throws IOException {
        InputStreamReader isr = new InputStreamReader(
                new FileInputStream(filename));
        //new FileInputStream("data\\test.gml"));
        BufferedReader br = new BufferedReader(isr);
        String line;
        boolean isNode = true;
        int i = 0;
        while ((line = br.readLine()) != null) {
            //			if(i%10000==0) {
            //				System.out.println(i);
            //			}
            //			i++;
//            if (line.contains("id ")) {
//                Integer id = Integer.valueOf(line.trim().split(" ")[1]);
//                HashSet<Integer> adjacentSet = new HashSet<Integer>();
//                G.put(id, adjacentSet);
//            } else if (line.contains("source")) {
            Integer source = Integer.valueOf(line.trim().split(" ")[0]);
            //line = br.readLine();
            Integer target = Integer.valueOf(line.trim().split(" ")[1]);
            if (G.get(source)==null){
                G.put(source, new HashSet<Integer>());
            }
            if (G.get(target)==null){
                G.put(target, new HashSet<Integer>());
            }
            G.get(source).add(target);
            G.get(target).add(source);



//            }
        }

    }


    private static void colMAdmin (int givennode, ServerThread serverThread,HashMap<Integer, HashSet<Integer>> G, String filename) throws Exception { //主网络处理

        //serverThread.broadcast(givennode); //1.广播给定节点,给定节点，需要在求并集之前广播，不是此处
        //2.初始化子网络的信息
        Integer ein = 0;
        Integer eout = G.get(givennode).size();
        ArrayList <Integer> C = new ArrayList();
        C.add(givennode);

        //3.全局网络的初始化
        int bestnode = givennode;
        double bestM ;
        double M = 0;
        //交互,得到的节点并集中的每个节点
        serverThread.candidateNodes.remove(givennode);
        while (True) {
            bestM = 0;
            for (Integer nodev : serverThread.candidateNodes) {
                //1. 发送给子网络一个节点id
                serverThread.broadcast(new EdgeReq(serverThread.getHandlerId(), nodev));
                //System.out.println("serverThread.candidateNodes"+serverThread.candidateNodes);
                //3. 计算将此节点与社区C后的内部边和外部边
                Integer[] temps = EinEout(nodev, ein, eout, C,G);
                int e_cCurrent = temps[0];
                int e_outCurrent = temps[1];
                EdgeRsp rsp=safeSumDispersion (nodev, e_cCurrent,  e_outCurrent,serverThread);//4. 将计算的内部边，外部边，分成随机数发送到其他子网络

                while(true){//5. 接收来自其他子网络的数据，
                    if (serverThread.EdgeRsps.size()>=(serverThread.clients().size()-1)){
                        break;
                    }else{
                        Thread.sleep(1000);
                    }
                }
                // 6. ein,eout分别求和，得到整个网络的内部边数及外部边数
                double sum_e_cCurrent = rsp.getInFrag();//初始化为当前网络的值
                double sum_e_outCurrent =rsp.getOutFrag();
                for (EdgeRsp temprsp: serverThread.EdgeRsps){
                    sum_e_cCurrent=sum_e_cCurrent+temprsp.getInFrag();
                    sum_e_outCurrent=sum_e_outCurrent+temprsp.getOutFrag();
                }
                int sum_e_cCurrentI = (int) Math.round(sum_e_cCurrent);
                int sum_e_outCurrentI = (int) Math.round(sum_e_outCurrent);
                serverThread.EdgeRsps.clear();

                //7.对比当前的M值，比当前的M值大，则更新，
                Double tempM=(double)Math.round((1.0 * sum_e_cCurrentI / sum_e_outCurrentI)*10000)/10000;
                if ( tempM> bestM ||(tempM.equals(bestM) && nodev<bestnode)) {
                    bestM = tempM;
                    bestnode = nodev;
                }
            }

            if (M <= bestM) {  //8. 广播加入社区中的节点bestnode,,
                C.add(bestnode);
                M = bestM;
                serverThread.candidateNodes.remove(bestnode);////将此节点从并集中删除
                serverThread.broadcast(new Best(bestnode)); //广播
                Integer[] temps = EinEout(bestnode, ein, eout, C,G);//更新当前子网络的ein, eout
                ein = temps[0];
                eout = temps[1];

                //判断是否扩展并集
                HashSet tempNeigh= (HashSet) G.get(bestnode).clone();
                tempNeigh.removeAll(serverThread.nodes);
                UnionExtend tempUnionflag=new UnionExtend(0);
                if (tempNeigh.size()>=1){//存在邻居不在并集中， tempUnionflag.flag置1
                    tempUnionflag.flag=1;
                }

                while(true){ //5. 接收来自其他子网络的数据，
                    if (serverThread.unionFlags.size()>=(serverThread.clients().size()-1)){
                        break;
                    }else{
                        Thread.sleep(1000);
                    }
                }

                for (UnionExtend temp : serverThread.unionFlags){
                    tempUnionflag.flag=tempUnionflag.flag+temp.flag;
                }
                serverThread.broadcast(tempUnionflag);

                if (tempUnionflag.flag>=1){//如果需要计算，启动计算并集程序

                    HashSet<Integer> k_neighbors = (HashSet<Integer>) G.get(bestnode).clone();
                    HashSet<Integer> tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                    for (int j = 1; j < k; j++) {
                        for (Integer node : tempneighbors) {
                            k_neighbors.addAll(G.get(node));
                        }
                        tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                    }
                    tempneighbors.addAll(serverThread.nodes);
                    serverThread.nodes.clear();
                    System.out.println("tempneighbors--2"+tempneighbors.toString());
                    ElGamalSafeUnionAdminNetwork(tempneighbors,nodesNum,serverThread);
                    serverThread.candidateNodes= (HashSet<Integer>) serverThread.nodes.clone();
                    serverThread.candidateNodes.removeAll(C);

                    serverThread.unionFlags.clear();
                }
            } else {
                System.out.println("foundComunities"+ C);
                writeCommunities (givennode,  C, filename);
                serverThread.broadcast(new End(1));
                break;  //发送程序终止
            }
        }
    }


    private static EdgeRsp safeSumDispersion (int node, int ein, int eout, ServerThread serverThread) throws IOException, InterruptedException {  //将内部边和外部边分散

        Random random = new Random();
        EdgeInfo edgeInfo = new EdgeInfo(node, ein, eout);//自己计算的边信息：节点信息，内部边数，外部边数
        EdgeFragment[] fragments = new EdgeFragment[3]; //随机分成3份
        fragments[0] = new EdgeFragment(node, edgeInfo.getIEdgeNum() == 0 ? random.nextDouble() : random.nextDouble() * edgeInfo.getIEdgeNum(), edgeInfo.getOEdgeNum() == 0 ? random.nextDouble() : random.nextDouble() * edgeInfo.getOEdgeNum());
        //如果等于0，随机浮点数，浮点数*边数
        fragments[1] = new EdgeFragment(node, edgeInfo.getIEdgeNum() == 0 ? random.nextDouble() : random.nextDouble() * edgeInfo.getIEdgeNum(), edgeInfo.getOEdgeNum() == 0 ? random.nextDouble() : random.nextDouble() * edgeInfo.getOEdgeNum());
        fragments[2] = new EdgeFragment(node, edgeInfo.getIEdgeNum() - fragments[0].getIFrag() - fragments[1].getIFrag(), edgeInfo.getOEdgeNum() - fragments[0].getOFrag() - fragments[1].getOFrag());

        int i = 1;  //将边信息发送给各个子网络
        for (ClientThread clientThread : serverThread.clients().values()) { //变量服务端连接
            if (!clientThread.handlerId.equals(serverThread.getHandlerId())) {  //如果不是自己
                clientThread.send(fragments[i]); //发送碎片
                if (++i > 2) {
                    break;
                }
            }
        }
        //接收来自其他网络的碎片
        while (true) {
            if (serverThread.EdgeFragments.size()>=(serverThread.handlers.size()-1)){
                break;
            }else{
                Thread.sleep(1000);
            }
        }
        EdgeRsp rsp = new EdgeRsp(0.0,0.0);//根据要计算的节点id获得计算的边数结果，把别人发送的边数也加上
        rsp.setInFrag(fragments[0].getIFrag());//当前网络的内部边数
        rsp.setOutFrag(fragments[0].getOFrag());//当前网络的外部边数
        for (EdgeFragment fragment:serverThread.EdgeFragments){
            rsp.setInFrag(rsp.getInFrag() +fragment.getIFrag());  //内部边数累加
            rsp.setOutFrag(rsp.getOutFrag() + fragment.getOFrag()); //外部边数累加
        }
        serverThread.EdgeFragments.clear();
        return rsp;
    }



    private static void colMsub (Integer givennode, ServerThread serverThread,HashMap<Integer, HashSet<Integer>> G) throws Exception {//子网络处理

        //2.初始化子网络的信息
        Integer ein = 0;
        Integer eout = G.get(givennode).size();
        ArrayList Ci = new ArrayList();
        Ci.add(givennode);

        while (True) {//接收数据，数据含有‘best’:更新，‘break’：跳出循环；"id":节点id；
            if (serverThread.EdgeReqs.size()==1){
                Integer[] temps = EinEout(serverThread.EdgeReqs.get(0).nodeId, ein, eout, Ci,G);
                int e_cCurrent = temps[0];
                int e_outCurrent = temps[1];
                EdgeRsp rsp=safeSumDispersion (serverThread.EdgeReqs.get(0).nodeId, e_cCurrent,  e_outCurrent,serverThread);//4. 将计算的内部边，外部边，分成随机数发送到其他子网络
                serverThread.clients().get("localhost:8000").send(rsp); //发送给协调节点
                serverThread.EdgeReqs.clear();

            }else if (!(serverThread.best==null)){ //如果接收最佳节点
                //8. 接收加入社区的节点bestnode,更新ein, eout
                Integer bestnode= serverThread.best.best;
                serverThread.best=null;
                Ci.add(bestnode);
                Integer[] temps  = EinEout(bestnode, ein, eout, Ci,G);
                ein = temps[0];
                eout= temps[1];

                //判断是否需要扩展交集
                HashSet tempNeigh= (HashSet) G.get(bestnode).clone();
                tempNeigh.removeAll(serverThread.nodes);
                UnionExtend tempUnionflag=new UnionExtend(0);
                if (tempNeigh.size()>=1){//存在邻居不在并集中，发送给1
                    tempUnionflag.flag=1;
                }
                serverThread.clients().get("localhost:8000").send( tempUnionflag);
                //接收来自admin的信息
                while(true){ //5. 接收来主网络的数据，标志是否向外扩展
                    if (serverThread.unionFlags.size()>=1){
                        break;
                    }else{
                        Thread.sleep(1000);
                    }
                }
                if (serverThread.unionFlags.get(0).flag>=1)
                {
                    HashSet<Integer> k_neighbors = (HashSet<Integer>) G.get(bestnode).clone();//重新计算交集
                    HashSet<Integer> tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                    for (int j = 1; j < k; j++) {
                        for (Integer node : tempneighbors) {
                            k_neighbors.addAll(G.get(node));
                        }
                        tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                    }
                    tempneighbors.addAll(serverThread.nodes);
                    serverThread.nodes.clear();
                    System.out.println("tempneighbors--2"+tempneighbors.toString());
                    ElGamalSafeUnionSubNetwork(tempneighbors,nodesNum,serverThread);
                }
                serverThread.unionFlags.clear();

            } else if (serverThread.endFlag.flag.equals(1)){ //如果接收到end，break
                System.out.println("收到节点结束--消息"+Ci);
                synchronized (serverThread.endFlag){
                    serverThread.endFlag=new End(0);
                }
                break;
            } else if (serverThread.endFlag.flag.equals(-1)){ //如果接收到end，break
                System.out.println("收到end消息");
                System.exit(0);

            }else{
                Thread.sleep(100);
            }

        }
    }


    private static Integer[]  EinEout (int nodev, int ein, int eout, ArrayList C,HashMap<Integer, HashSet<Integer>> G) throws IOException {
        Integer[]  edgenums;
        if (G.containsKey(nodev)){
            int dv = G.get(nodev).size();
            HashSet temp=new HashSet(C);
            HashSet x =  (HashSet) G.get(nodev).clone();
            x.retainAll(temp);
            int x1=x.size();
            int y = dv - x1;
            int e_cCurrent = ein + x1;
            int e_outCurrent = eout - x1 + y;
            edgenums= new Integer[]{e_cCurrent, e_outCurrent};
        }else{
            edgenums= new Integer[]{ein, eout};
        }
        return edgenums;
    }

        // Initial two graph
    public static void conGragh(String filename,double x, double y,double z, int seed,HashMap<Integer, HashSet<Integer>> G) throws IOException {//将一个网络拆分成3个网络
     //x是存在于所有网络的边的比例，y是属于属于两个网络的边的比例，z是只属于一个网络的边的比例，x=0.1, y=0.2, z=0.1,每个自网络所有的边比例：x+2y(与两个网络都有交集)+z=0.6

        InputStreamReader isr = new InputStreamReader(
                new FileInputStream(filename));
        //new FileInputStream("data\\test.gml"));

        ArrayList<Integer[]> edge_data=new ArrayList<>();

        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            if (line.contains("id ")) {
                Integer id = Integer.valueOf(line.trim().split(" ")[1]);
                HashSet<Integer> adjacentSet = new HashSet<Integer>();
                G.put(id, adjacentSet);
            } else if (line.contains("source")) {
                Integer source = Integer.valueOf(line.trim().split(" ")[1]);
                line = br.readLine();
                Integer target = Integer.valueOf(line.trim().split(" ")[1]);
                G.get(source).add(target);
                G.get(target).add(source);
                Integer[] temps={source,target};
                temps[0]=source;
                temps[1]=target;
                edge_data.add(temps);
            }
        }


        ArrayList<Integer[]> graghEdge1 = new ArrayList<>();
        ArrayList<Integer[]> graghEdge2 = new ArrayList<>();
        ArrayList<Integer[]> graghEdge3 = new ArrayList<>();

        Random r1 = new Random();
        r1.setSeed(seed);
        // Assign edges to two networks, each of which must exist in at least one network
        for (int i = 0; i < edge_data.size(); i++) {
            double rand1 = r1.nextDouble();
            if (rand1<=x) {
                graghEdge1.add(edge_data.get(i));
                graghEdge2.add(edge_data.get(i));
                graghEdge3.add(edge_data.get(i));
            }
            else if (rand1 > x && rand1 <= x+y) {
                graghEdge1.add(edge_data.get(i));
                graghEdge2.add(edge_data.get(i));
            }
            else if (rand1 > x+y && rand1 <= x+2*y) {
                graghEdge1.add(edge_data.get(i));
                graghEdge3.add(edge_data.get(i));
            }
            else if (rand1 > x+2*y && rand1 <= x+3*y) {
                graghEdge2.add(edge_data.get(i));
                graghEdge3.add(edge_data.get(i));
            }
            else if (rand1 > x+3*y && rand1 <= x+3*y+z) {
                graghEdge1.add(edge_data.get(i));
            }
            else if (rand1 >x+3*y+z && rand1 <= x+3*y+2*z) {
                graghEdge2.add(edge_data.get(i));
            }
            else {
                graghEdge3.add(edge_data.get(i));
            }
        }
        //写入文件，一个图的边写入一个文件
        File file = new File("E:\\JAVA\\MultiNetwork-zhenghe\\src\\main\\java\\me\\xuxiaoxiao\\multinetwork\\data\\G1.txt");
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        PrintStream oos = new PrintStream(fos);
        for (Integer[] edge: graghEdge1){
            oos.println(edge[0]+" "+edge[1]);
            //System.out.println(edge[0]+" "+edge[1]+"\n");
        }
        oos.flush();
        oos.close();
        fos.close();
        System.out.println("G1");

        //写入文件，一个图的边写入一个文件
        file = new File("E:\\JAVA\\MultiNetwork-zhenghe\\src\\main\\java\\me\\xuxiaoxiao\\multinetwork\\data\\G2.txt");
        file.createNewFile();
        fos = new FileOutputStream(file);
        oos = new PrintStream(fos);
        for (Integer[] edge: graghEdge2){
            oos.println(edge[0]+" "+edge[1]);
            //System.out.println(edge[0]+" "+edge[1]+"\n");
        }
        oos.flush();
        oos.close();
        fos.close();
        System.out.println("over");

        //写入文件，一个图的边写入一个文件
        file = new File("E:\\JAVA\\MultiNetwork-zhenghe\\src\\main\\java\\me\\xuxiaoxiao\\multinetwork\\data\\G3.txt");
        file.createNewFile();
        fos = new FileOutputStream(file);
        oos = new PrintStream(fos);
        for (Integer[] edge: graghEdge3){
            oos.println(edge[0]+" "+edge[1]);
            //System.out.println(edge[0]+" "+edge[1]+"\n");
        }
        oos.flush();
        oos.close();
        fos.close();
        System.out.println("over");

    }


    private static void writeCommunities(Integer givennode, ArrayList<Integer> C, String filename) throws IOException {
        File file=new File(filename);
        if (!file.exists()){
            file.createNewFile();
        }
        FileOutputStream  fos=new FileOutputStream(file,true);
        PrintStream oos=new PrintStream(fos);
        oos.print(givennode+":");
        for (Integer node:C){
            oos.print(node+" ");
        }
        oos.print("\n");
        oos.flush();
        oos.close();
    }



}
