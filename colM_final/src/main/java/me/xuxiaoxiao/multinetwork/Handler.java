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
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.Security;
import java.util.*;

public class Handler {
    public static final Gson GSON = new Gson();

    public static final String CFG_PREFIX = "me.xuxiaoxiao$multinetwork$";
    public static final String CFG_SERVER_PORT = CFG_PREFIX + "server.port";
    public static final String WORK_DIR = ".";


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
    private static Integer k=1;
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
        String[] graphNames = {"3sources", "BBC4view_685", "UCI_mfeat","HIE", "PEP"};
        //本代码是模拟3方的版本。在实验中，UCI_mfeat数据集我们使用的是6个网络，实验时需要自行更改。
        //对于四个人工数据集分解的结果，我们五组数据位于文件夹的0-4中
        HashMap<Integer, HashSet<Integer>> G = new HashMap<Integer, HashSet<Integer>>();

        boolean admin = Boolean.parseBoolean(args[2]);//是否是管理机器
        System.out.println("start handle");
        String filename=null;
        String writeFilename=null;
        for (String graphName:graphNames) {
            if (serverThread.handlerId.equals("localhost:8000")) {
                filename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\" + graphName + "\\G1.txt";
                writeFilename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\" + graphName + "\\res_加密版.txt";

                System.out.println("read G1");
            } else if (serverThread.handlerId.equals("localhost:8001")) {
                filename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\" + graphName + "\\G2.txt";
                writeFilename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\" + graphName + "\\G2Communities.txt";

                System.out.println("read G2");
            } else if (serverThread.handlerId.equals("localhost:8002")) {
                filename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\" + graphName + "\\G3.txt";
                writeFilename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\" + graphName + "\\G3Communities.txt";

                System.out.println("read G3");
            }
            serialize(filename, G);
            //conGragh(filename,0.1, 0.2,0.1,0);
            ArrayList<Integer> givenNodeList = new ArrayList<>();
            //givenNodeList.add( 0);
            //获取给定节点
            try {
                BufferedReader br = new BufferedReader(new FileReader(Handler.WORK_DIR + "\\src\\main\\resources\\data\\" + graphName + "\\nodes.txt"));
                String line;

                while ((line = br.readLine()) != null) {
                    // 将每一行转换为整数并添加到 ArrayList 中
                    int node = Integer.parseInt(line.trim());
                    givenNodeList.add(node);
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(givenNodeList);
            //Integer givennode = 0;要不要块状计算，避免多次通信
            // 对每一个给定节点执行算法
            HashSet<Integer> tempneighbors;
            for (Integer givennode : givenNodeList) {
                System.out.println("start givennode" + givennode);
                if (G.containsKey(givennode)) {
                    // 图G中存在节点givennode
                    HashSet<Integer> k_neighbors = (HashSet<Integer>) G.get(givennode).clone();
                    tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                    for (int j = 1; j < k; j++) {
                        // 可以获得k跳邻居，本文k=1,未执行
                        for (Integer node : tempneighbors) {
                            k_neighbors.addAll(G.get(node));
                        }
                        tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                        System.out.println("tempneighbors" + tempneighbors);
                    }
                }
                else {
                    tempneighbors = new HashSet<Integer>();
                    System.out.println("null" + tempneighbors);
                }
                if (admin) {
                    while (true) {  //广播
                        Thread.sleep(1000);
                        if (serverThread.clients().size() >= 3) { //有3个节点开始处理
                            //主方执行的代码
                            ElGamalSafeUnionAdminNetwork(tempneighbors, nodesNum, serverThread);

                            colMAdmin(givennode, serverThread, G, writeFilename);
                            break;//有3个节点开始处理结束后跳出循环
                        }
                    }
                } else {
                    //接收来自admin网络的p,g
                    //从方执行的代码
                    ElGamalSafeUnionSubNetwork(tempneighbors, nodesNum, serverThread);
                    colMsub(givennode, serverThread, G);
                    //break;

                }
                System.out.println("finish one node");
            }
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

    public static void ElGamalSafeUnionSubNetwork( HashSet<Integer> tempneighbors, Integer nodesNum,ServerThread serverThread) throws Exception {
        //子网络使用的安全多方集并集协议
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
        //主网络使用的安全多方集并集协议
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

        //初始化子网络的信息
        Integer ein = 0;
        Integer eout;
        if (G.containsKey(givennode)) {
            eout = G.get(givennode).size();
        } else {
            eout = 0;
        }
        ArrayList <Integer> C = new ArrayList();
        C.add(givennode);

        //全局网络的初始化
        double bestM ;
        double M = 0;

        //记录一些过程，无实际用处
        int preEin=0, preEout=0, dEin = 0, dEout = 0, tempEin = 0, tempEout=0, degree = 0;

        serverThread.candidateNodes.remove(givennode);
        while (True) {
            bestM = -1;
            Map<Integer, Double> node_M = new HashMap<>();

            //对每个邻居节点计算加入社区后的模块度，选择能使得社区模块度增加的节点存入node_M作为候选节点
            for (Integer nodev : serverThread.candidateNodes) {
                //1. 发送给子网络一个节点id
                serverThread.broadcast(new EdgeReq(serverThread.getHandlerId(), nodev));
                //System.out.println("serverThread.candidateNodes"+serverThread.candidateNodes);
                //3. 计算将此节点与社区C后的内部边和外部边
                Integer[] temps = EinEout(nodev, ein, eout, C,G);
                int e_cCurrent = temps[0];
                int e_outCurrent = temps[1];
                EdgeRsp rsp=safeSumDispersion (e_cCurrent,  e_outCurrent, serverThread);//4. 将计算的内部边，外部边，分成随机数发送到其他子网络
                //System.out.println("admin_rsp:"+rsp.getInFrag()+" "+rsp.getOutFrag());
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

                dEin = sum_e_cCurrentI - preEin;
                dEout = sum_e_outCurrentI - preEout;
                degree =  2 * dEin + dEout;

                //7.对比当前的M值，比当前的M值大，则更新，
                Double tempM = (double) Math.round((1.0 * sum_e_cCurrentI / sum_e_outCurrentI) * 10000) / 10000;
                System.out.println("nodev:" + nodev + ", degree:" + degree + ", ein:" + sum_e_cCurrentI + ", eout:" + sum_e_outCurrentI + ", M:" + tempM + ", dein:" + dEin + ", deout:" + dEout + ", dM:" + (tempM - M));
                //将能增加社区模块度的节点加入候选节点集
                if (tempM - M > 0){
                    node_M.put(nodev, tempM);
                }
            }
            // 挑选M大的一半的候选节点，并格式化数据
            List<List<Integer>> nodess = format_data(node_M);

            List<Integer> bestnodes = null;

            for (List<Integer> nodevs : nodess) {
                //发送给子网络一个节点id
                serverThread.broadcast(new EdgessReq(serverThread.getHandlerId(), nodevs));
                //计算将此节点集合与社区C后的内部边和外部边
                Integer[] temps = EinEoutss(nodevs, ein, eout, C,G);
                int e_cCurrent = temps[0];
                int e_outCurrent = temps[1];
                //使用安全求和协议
                EdgeRsp rsp=safeSumDispersion (e_cCurrent,  e_outCurrent,serverThread);//4. 将计算的内部边，外部边，分成随机数发送到其他子网络

                while(true){//接收来自其他子网络的数据，
                    if (serverThread.EdgeRsps.size()>=(serverThread.clients().size()-1)){
                        break;
                    }else{
                        Thread.sleep(1000);
                    }
                }
                // ein,eout分别求和，得到整个网络的内部边数及外部边数
                double sum_e_cCurrent = rsp.getInFrag();//初始化为当前网络的值
                double sum_e_outCurrent =rsp.getOutFrag();
                for (EdgeRsp temprsp: serverThread.EdgeRsps){
                    sum_e_cCurrent=sum_e_cCurrent+temprsp.getInFrag();
                    sum_e_outCurrent=sum_e_outCurrent+temprsp.getOutFrag();
                }
                int sum_e_cCurrentI = (int) Math.round(sum_e_cCurrent);
                int sum_e_outCurrentI = (int) Math.round(sum_e_outCurrent);
                serverThread.EdgeRsps.clear();

                dEin = sum_e_cCurrentI - preEin;
                dEout = sum_e_outCurrentI - preEout;

                Double tempM = (double) Math.round((1.0 * sum_e_cCurrentI / sum_e_outCurrentI) * 10000) / 10000;
                System.out.println("nodevs:" + nodevs + ", ein:" + sum_e_cCurrentI + ", eout:" + sum_e_outCurrentI  + ", M:" + tempM + ", dein:" + dEin + ", deout:" + dEout +  ", dM:" + (tempM - M));

                //对比当前的M值，比当前的M值大，则更新，
                if (tempM >= bestM) {
                    bestM = tempM;
                    tempEin = sum_e_cCurrentI;
                    tempEout = sum_e_outCurrentI;
                    bestnodes = nodevs;
                }
            }

            // 这里是输出到控制台，方便观看，无实际效果
            if (bestnodes != null && !bestnodes.isEmpty()) {
                StringBuilder output = new StringBuilder();
                for (Integer num : bestnodes) {
                    output.append(num).append(" ");
                }
                System.out.println("bestnodee: " + output.toString());
            }

            // 将bestnodes加入社区中
            if (bestnodes != null && !bestnodes.isEmpty()) {  //广播加入社区中的节点bestnode,,
                preEin = tempEin;
                preEout = tempEout;
                M = bestM;

                C.addAll(bestnodes);
                serverThread.candidateNodes.removeAll(bestnodes);
                serverThread.broadcast(new Bests(bestnodes)); //广播
                Integer[] temps = EinEoutss(bestnodes, ein, eout, C,G);//更新当前子网络的ein, eout
                ein = temps[0];
                eout = temps[1];

                //判断是否扩展并集
                UnionExtend tempUnionflag = new UnionExtend(0);
                HashSet<Integer> tempNeigh = new HashSet<>();
                for(Integer vvv:bestnodes){
                    if(G.containsKey(vvv)){
                        HashSet<Integer> N_v = G.get(vvv);
                        tempNeigh.addAll(N_v);
                    }
                }
                tempNeigh.removeAll(serverThread.nodes); //邻居节点，除去已访问的节点和社区中的节点
                tempNeigh.removeAll(C);
                if (tempNeigh.size() >= 1) {//存在邻居不在并集中， tempUnionflag.flag置1
                    tempUnionflag.flag = 1;
                }else {
                    tempUnionflag.flag = 0;
                }

                while(true){ //接收来自其他子网络的数据，
                    if (serverThread.unionFlags.size()>=(serverThread.clients().size()-1)){
                        break;
                    }else{
                        Thread.sleep(1000);
                    }
                }

                for (UnionExtend temp : serverThread.unionFlags){
                    tempUnionflag.flag=tempUnionflag.flag+temp.flag;
                }
                serverThread.unionFlags.clear();
                serverThread.broadcast(tempUnionflag);

                if (tempUnionflag.flag>=1){//如果需要计算，启动计算并集程序

                    HashSet<Integer> tempneighbors;
                    HashSet<Integer> k_neighbors = new HashSet<>();
                    for(Integer vvv:bestnodes){
                        if (G.containsKey(vvv)) {
                            HashSet<Integer> N_v = G.get(vvv);
                            k_neighbors.addAll(N_v);
                        }
                    }
                    k_neighbors.removeAll(bestnodes);
                    tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
//                    for (int j = 1; j < k; j++) {
//                        for (Integer node : tempneighbors) {
//                            k_neighbors.addAll(G.get(node));
//                        }
//                        tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
//                    }
                    tempneighbors.removeAll(C);
                    if (k_neighbors.size() == 0){
                        tempneighbors = new HashSet<Integer>();
                    }
                    tempneighbors.addAll(serverThread.nodes);
                    serverThread.nodes.clear();
                    System.out.println("tempneighbors--2"+tempneighbors.toString());
                    ElGamalSafeUnionAdminNetwork(tempneighbors,nodesNum,serverThread);
                    serverThread.candidateNodes= (HashSet<Integer>) serverThread.nodes.clone();
                    serverThread.candidateNodes.removeAll(C);


                }
            } else {
                System.out.println("foundComunities"+ C);
                writeCommunities (givennode,  C, filename);
                serverThread.broadcast(new End(1));
                break;  //发送程序终止
            }
        }
    }


    private static EdgeRsp safeSumDispersion (int ein, int eout, ServerThread serverThread) throws IOException, InterruptedException {  //将内部边和外部边分散
        //安全求和协议
        Random random = new Random();
        EdgeInfo edgeInfo = new EdgeInfo(0, ein, eout);//自己计算的边信息：节点信息，内部边数，外部边数
        EdgeFragment[] fragments = new EdgeFragment[3]; //随机分成3份
        fragments[0] = new EdgeFragment(0, edgeInfo.getIEdgeNum() == 0 ? random.nextDouble() : random.nextDouble() * edgeInfo.getIEdgeNum(), edgeInfo.getOEdgeNum() == 0 ? random.nextDouble() : random.nextDouble() * edgeInfo.getOEdgeNum());
        //如果等于0，随机浮点数，浮点数*边数
        fragments[1] = new EdgeFragment(0, edgeInfo.getIEdgeNum() == 0 ? random.nextDouble() : random.nextDouble() * edgeInfo.getIEdgeNum(), edgeInfo.getOEdgeNum() == 0 ? random.nextDouble() : random.nextDouble() * edgeInfo.getOEdgeNum());
        fragments[2] = new EdgeFragment(0, edgeInfo.getIEdgeNum() - fragments[0].getIFrag() - fragments[1].getIFrag(), edgeInfo.getOEdgeNum() - fragments[0].getOFrag() - fragments[1].getOFrag());

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

        //初始化子网络的信息
        Integer ein = 0;
        Integer eout;
        if (G.containsKey(givennode)) {
            eout = G.get(givennode).size();
        } else {
            eout = 0;
        }

        ArrayList Ci = new ArrayList();
        Ci.add(givennode);

        while (True) {//接收数据，数据含有‘best’:更新，‘break’：跳出循环；"id":节点id；
            if (serverThread.EdgessReqs.size() == 1) {// 接受请求，发送内外部边
                Integer[] temps = EinEoutss(serverThread.EdgessReqs.get(0).nodesId, ein, eout, Ci, G);
                int e_cCurrent = temps[0];
                int e_outCurrent = temps[1];
                EdgeRsp rsp = safeSumDispersion(e_cCurrent, e_outCurrent, serverThread);//4. 将计算的内部边，外部边，分成随机数发送到其他子网络
                serverThread.clients().get("localhost:8000").send(rsp); //发送给协调节点
                serverThread.EdgessReqs.clear();
                System.out.println("rsp"+rsp.getInFrag());
                System.out.println("rsp"+rsp.getOutFrag());

            }
            else if (serverThread.EdgeReqs.size()==1){
                Integer[] temps = EinEout(serverThread.EdgeReqs.get(0).nodeId, ein, eout, Ci,G);
                int e_cCurrent = temps[0];
                int e_outCurrent = temps[1];
                EdgeRsp rsp=safeSumDispersion (e_cCurrent,  e_outCurrent,serverThread);//4. 将计算的内部边，外部边，分成随机数发送到其他子网络
                serverThread.clients().get("localhost:8000").send(rsp); //发送给协调节点
                serverThread.EdgeReqs.clear();
                System.out.println("rsp"+rsp.getInFrag());
                System.out.println("rsp"+rsp.getOutFrag());

            }
            else if (!(serverThread.bests==null)){ //如果接收最佳节点
                //接收加入社区的节点集bestnodes,更新ein, eouts
                List<Integer> bestnodes = serverThread.bests.bests; //best.best
                serverThread.bests=null;
                Ci.addAll(bestnodes);
                Integer[] temps  = EinEoutss(bestnodes, ein, eout, Ci,G);
                ein = temps[0];
                eout= temps[1];

                //判断是否需要扩展交集
                HashSet<Integer> tempNeigh = new HashSet<>();
                for(Integer vvv:bestnodes){
                    if(G.containsKey(vvv)){
                        HashSet<Integer> N_v = G.get(vvv);
                        tempNeigh.addAll(N_v);
                    }
                }
                tempNeigh.removeAll(serverThread.nodes);
                tempNeigh.removeAll(Ci);

                UnionExtend tempUnionflag=new UnionExtend(0);
                if (tempNeigh.size() >= 1) {//存在邻居不在并集中， tempUnionflag.flag置1
                    tempUnionflag.flag = 1;
                }else {
                    tempUnionflag.flag = 0;
                }
                System.out.println("tempUnionflag："+tempUnionflag);
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
                    HashSet<Integer> tempneighbors;
                    HashSet<Integer> k_neighbors = new HashSet<>();
                    for(Integer vvv:bestnodes){
                        if(G.containsKey(vvv)) {
                            HashSet<Integer> N_v = G.get(vvv);
                            k_neighbors.addAll(N_v);
                        }
                    }
                    k_neighbors.removeAll(bestnodes);
                    tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
//                    for (int j = 1; j < k; j++) {
//                        for (Integer node : tempneighbors) {
//                            k_neighbors.addAll(G.get(node));
//                        }
//                        tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
//                    }
                    tempneighbors.removeAll(Ci);
                    if (k_neighbors.size() == 0){
                        tempneighbors = new HashSet<Integer>();
                    }
                    tempneighbors.addAll(serverThread.nodes);
                    tempneighbors.removeAll(Ci);
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
        // 计算加入节点nodev后社区的内外边
        Integer[]  edgenums;
        if (G.containsKey(nodev)){
            int dv = G.get(nodev).size();//节点的度
            HashSet temp=new HashSet(C);//社区
            HashSet x =  (HashSet) G.get(nodev).clone();//节点的邻居集合
            x.retainAll(temp);//交集
            int x1=x.size();//邻居在社区内的数目
            int y = dv - x1;//邻居在社区外的数目
            int e_cCurrent = ein + x1;// 原内部边+新的内部边
            int e_outCurrent = eout - x1 + y;//外部边减去变成内部边的数目+新的外部边
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
        //写文件
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
    private static int getNodevsEdge(List<Integer> nodevs, HashMap<Integer, HashSet<Integer>> G) {
        //与论文中公式最后一项对应，计算节点集之间的边数
        int edgeCount = 0;

        // 遍历节点集合中的每对节点
        for (int i = 0; i < nodevs.size(); i++) {
            int node1 = nodevs.get(i);

            for (int j = i + 1; j < nodevs.size(); j++) {
                int node2 = nodevs.get(j);

                // 检查两个节点是否相连，如果相连则增加边的数量
                if (G.containsKey(node1) && G.get(node1).contains(node2)) {
                    edgeCount++;
                }
            }
        }

        return edgeCount;
    }

    private static int getNodevsSum(List<Integer> nodevs, List<Integer> C, HashMap<Integer, HashSet<Integer>> G) {
        //论文中公式求和部分对应
        int sum = 0;
        HashSet<Integer> nodevsUnionC = new HashSet<>(C);
        nodevsUnionC.addAll(nodevs);
        for(Integer nodev:nodevs){
            if (G.containsKey(nodev)) {
                HashSet<Integer> N_v = G.get(nodev);
                HashSet<Integer> intersectionSet = new HashSet<>(N_v);
                intersectionSet.retainAll(nodevsUnionC);
                sum += intersectionSet.size();
            }
        }
        return sum;
    }

    private static Integer[] EinEoutss(List<Integer> nodevs, int ein, int eout, List<Integer> C, HashMap<Integer, HashSet<Integer>> G) throws IOException {
        // 计算加入节点集nodevs后社区的内外边
        int nodevsEdge = getNodevsEdge(nodevs, G);
        int nodevsSum = getNodevsSum(nodevs, C, G);
        int e_cCurrent = ein + nodevsSum - nodevsEdge;
        int sumN_v = 0;
        for (int nodev : nodevs) {
            if(G.containsKey(nodev)) {
                HashSet<Integer> N_v = G.get(nodev);
                sumN_v += N_v.size();
            }
        }
        int e_outCurrent = eout + sumN_v - 2 * (nodevsSum - nodevsEdge);
        return new Integer[]{e_cCurrent, e_outCurrent};
    }


    private static List<List<Integer>> format_data(Map<Integer, Double> node_M) {
        //按M排序，挑选前一半社区，并格式化为多个节点集合组成的array

        System.out.println("len(node_M): "+ node_M.size());
//        for (Map.Entry<Integer, Double> entry : node_M.entrySet()) {
//            Integer key = entry.getKey();
//            Double value = entry.getValue();
//            System.out.println("Key: " + key + ", Value: " + value);
//        }

        List<Map.Entry<Integer, Double>> list = new ArrayList<>(node_M.entrySet());
        // 使用比较器按值从大到小排序
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                // 降序排序
                return Double.compare(o2.getValue(), o1.getValue());
            }
        });

        int halfSize = (int) Math.ceil((double) list.size() / 2);
        List<Map.Entry<Integer, Double>> vlist = list.subList(0, halfSize);
        List<List<Integer>> res = new ArrayList<>();
        List<Integer> temp = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : vlist) {
            //System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
            temp.add(entry.getKey());
            //System.out.println(temp);
            res.add(new ArrayList<>(temp));
        }
        System.out.println(res);
        return res;
    }

}
