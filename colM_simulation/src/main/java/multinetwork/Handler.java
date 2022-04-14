package multinetwork;

import com.google.gson.Gson;
import me.xuxiaoxiao.xtools.common.XTools;
import me.xuxiaoxiao.xtools.common.config.XConfigTools;
import multinetwork.base.ServerThread;
import multinetwork.msg.*;

import java.io.*;
import java.util.*;

public class Handler {
    public static final String WORK_DIR = ".";
    public static final Gson GSON = new Gson();

    public static final String CFG_PREFIX = "me.xuxiaoxiao$multinetwork$";

    private static boolean True = true;
    private static Integer k = 1;
    private static Integer nodesNum = 1000;

    public static void main(String[] args) throws Exception {
        XConfigTools.X_CONFIGS.cfgLoad("config.properties", "utf-8");

        Scanner scanner = new Scanner(System.in);
        //System.out.println("请输入服务端ID：使用 IP:端口");//自己的端口号：localhost, 端口：8100
        ServerThread serverThread = new ServerThread(args[0]);
        serverThread.start();

        //  System.out.println("请输入要连接的 IP:端口，直接按回车键跳过");
        String address = args[1];
        if (!XTools.strBlank(address)) {//别人的，要连接的地址，XTools.strBlank，判断地址是否为空
            serverThread.connect(address); //建立连接
        }

        HashMap<Integer, HashSet<Integer>> G = new HashMap<Integer, HashSet<Integer>>();
        HashMap<Integer, HashSet<Integer>> originalG = new HashMap<Integer, HashSet<Integer>>();

        boolean admin = Boolean.parseBoolean(args[2]);//是否是管理机器
        System.out.println("start handle");
        String graphName ="football";//"karate";//"polbooks";//"karate";//"dkpol";//"PNP"ok;//"MIX"ok; //// "dolphins"; //"strike";//

        String filename = null;//Handler.WORK_DIR + "\\src\\main\\resources\\data\\karate\\karate.gml";
        String writeFilename = null;
        String nodesFilename = null;
        //String patitionGraphpath=Handler.WORK_DIR+
        //conGragh(graphName,0.1, 0.2,0.1,1,G); //seed=0
        String flag=""; //flag=1表示是真实数据集拆分的，flag=0表示是人工数据集，用于区别写入文件夹的名称，人工数据集的话，flag置为空

        double p1=0.8;
        double p2=0.8;
        double p3=0.6;
        for (int i=0; i<10;i++){
            G.clear();
            originalG.clear();
            if (serverThread.handlerId.equals("localhost:8100")) {
                conGragh(graphName, i, p1, p2, p3, originalG); //i是seed，也是第i个网络,协调者划分网络
            }
            else{
                Thread.sleep(1000); //非协调者，等待
            }
            flag=String.valueOf(i);

            nodesFilename=Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+graphName+ "\\"+ String.valueOf(p1)+ String.valueOf(p2)+String.valueOf(p3)+"\\"+flag+"\\nodes.txt";
            if (serverThread.handlerId.equals("localhost:8100")) {
                filename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+graphName+ "\\"+ String.valueOf(p1)+ String.valueOf(p2)+String.valueOf(p3)+"\\"+flag+"\\G1.txt";
                writeFilename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+graphName+ "\\"+ String.valueOf(p1)+ String.valueOf(p2)+String.valueOf(p3)+"\\"+flag+"\\G1Communities.txt";

                System.out.println("read G1");
            } else if (serverThread.handlerId.equals("localhost:8101")) {
                filename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+graphName+ "\\"+ String.valueOf(p1)+ String.valueOf(p2)+String.valueOf(p3)+"\\"+flag+"\\G2.txt";
                writeFilename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+graphName+ "\\"+ String.valueOf(p1)+ String.valueOf(p2)+String.valueOf(p3)+"\\"+flag+"\\G2Communities.txt";

                System.out.println("read G2");
            } else if (serverThread.handlerId.equals("localhost:8102")) {
                filename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+graphName+ "\\"+ String.valueOf(p1)+ String.valueOf(p2)+String.valueOf(p3)+"\\"+flag+"\\G3.txt";
                writeFilename = Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+graphName+ "\\"+ String.valueOf(p1)+ String.valueOf(p2)+String.valueOf(p3)+"\\"+flag+"\\G3Communities.txt";

                System.out.println("read G3");
            }
            HashSet<Integer> nodes = new HashSet<>();
            serialize(filename,nodesFilename, G, nodes);
            System.out.println("nodes"+nodes);
            ArrayList<Integer> nodeslist=new ArrayList<Integer>(nodes);
            Collections.sort(nodeslist);
            HashSet<Integer> selectednodes= new HashSet<>();
            for (int temp=0; temp<=nodes.size(); temp++){
                if (temp%3==0){  //取一半的节点
                    selectednodes.add(nodeslist.get(temp));

                }

            }

            //ArrayList<Integer> givenNodeList = new ArrayList<>();
    //        nodes.clear();
    //        for (int i =60; i <=100; i++){
    //            nodes.add(i);
    //        }
            //Integer givennode = 0;要不要块状计算，避免多次通信
    //        nodes.clear();
    //        nodes.add(51);
            for (Integer givennode : selectednodes) {
                System.out.println("node: " + givennode);
                HashSet<Integer> tempneighbors;
                if (G.containsKey(givennode)) {
                    HashSet<Integer> k_neighbors = (HashSet<Integer>) G.get(givennode).clone();
                    tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                    for (int j = 1; j < k; j++) {
                        System.out.println("k: " + k);
                        for (Integer node : tempneighbors) {
                            k_neighbors.addAll(G.get(node));
                        }
                        tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                        //System.out.println("tempneighbors"+tempneighbors);
                    }
                    tempneighbors.remove(givennode);  // 删除给定节点
                } else {
                    tempneighbors = new HashSet<Integer>();
                    System.out.println("null" + tempneighbors);
                }
                System.out.println("admin" + admin);
                if (admin) {
                    while (true) {  //广播
                        Thread.sleep(1000);
                        if (serverThread.clients().size() >= 3) { //有3个节点开始处理
                            //serverThread.broadcast(new EdgeReq(serverThread.getHandlerId(), "fff"));·
                            ElGamalSafeUnionAdminNetwork(tempneighbors, nodesNum, serverThread);

                            colMAdmin(givennode, serverThread, G, writeFilename);
                            //serverThread.broadcast(new EdgeReq(serverThread.getHandlerId(), "fff"));
                            break; //有3个节点开始处理结束后跳出循环
                        }
                    }
                } else {
                    //接收来自admin网络的p,g
                    ElGamalSafeUnionSubNetwork(tempneighbors, nodesNum, serverThread);
                    colMsub(givennode, serverThread, G);
                    //break;

                }
                System.out.println("finish one node");
                //break;
            }


        }
        serverThread.broadcast(new End(-1));//代码运行完,flag置为-1
        System.out.println("发送end消息，下一个节点");
        System.exit(0);

        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
    }


    public static void ElGamalSafeUnionSubNetwork(HashSet<Integer> tempneighbors, Integer nodesNum, ServerThread serverThread) throws Exception {

        //从前一个客户端接收邻居localNeighbors，并上当前节点的邻居，
        LocalNeighbors localNeighbors = new LocalNeighbors(new HashSet<Integer>(serverThread.localNeighbors.Neighbors));
        //System.out.println("received uv"+uv);
        while (True) {
            if (serverThread.localNeighbors.flag != 0) {  //0表示没有被赋值，1表示被赋值
                synchronized (serverThread.localNeighbors) {
                    localNeighbors.Neighbors.addAll(serverThread.localNeighbors.Neighbors);
                }
                serverThread.localNeighbors = new LocalNeighbors(new HashSet<Integer>()); //清空serverThread.uv;  //清空，以供下次循环使用
                break;
            } else {
                Thread.sleep(1000);
            }
        }
        serverThread.localNeighbors.flag=0;
        localNeighbors.getNeighbors().addAll(tempneighbors);  //接收的数据并上当前网络的数据
        //邻居传输给下一个客户端
        System.out.println("serverThread.handlerId:" + serverThread.handlerId);
        if (serverThread.handlerId.equals("localhost:8101")) {
            serverThread.handlers.get("localhost:8102").send(localNeighbors);
        } else if (serverThread.handlerId.equals("localhost:8102")) {
            serverThread.handlers.get("localhost:8100").send(localNeighbors);
            //serverThread.uv=new UV(new ArrayList(),new ArrayList()); //清空serverThread.uv;
        }
        System.out.println("waiting"+localNeighbors);
        //等待协调节点发送并集节点
        //System.out.println("等待协调节点发送并集节点"+serverThread.nodes.size());
        while (True) {

            if (serverThread.nodes.size() >= 1) {
                serverThread.candidateNodes = (HashSet<Integer>) serverThread.nodes.clone();
                serverThread.nodes = new HashSet<>(); //serverThread.nodes.需要清空吗？
                break;
            } else {
                Thread.sleep(100);
            }
        if (serverThread.endFlag.flag.equals(1)) { //如果接收到end，break
                System.out.println("孤立点") ; //serverThread.endFlag.flag.equals(1)在colM更新
                break;
            }

        }

    }

    public static void ElGamalSafeUnionAdminNetwork(HashSet<Integer> tempneighbors, Integer nodesNum, ServerThread serverThread) throws Exception {
        /////不带有加密过程
        //给下一个客户端发送邻居节点：tempneighbors，----待改，加密后的信息传输给下一个客户端
        LocalNeighbors templocalNeighbors = new LocalNeighbors(new HashSet<Integer>(tempneighbors));
        serverThread.handlers.get("localhost:8101").send(templocalNeighbors);

        //11. 接收来自最后一个客户端发来的邻居并集
        LocalNeighbors localNeighbors;

        synchronized (serverThread.localNeighbors) {
            localNeighbors = serverThread.localNeighbors;
        }
        while (True) {
            if (localNeighbors.flag != 0) {
                break;
            } else {
                Thread.sleep(1000);
                synchronized (serverThread.localNeighbors) {
                    localNeighbors = serverThread.localNeighbors;
                    serverThread.localNeighbors = new LocalNeighbors(new HashSet<Integer>());
                }
            }
        }
        //获得并集并广播（求所有网络的k_neighbors的并集）
        serverThread.candidateNodes = localNeighbors.getNeighbors();
        serverThread.broadcast(serverThread.candidateNodes);
        serverThread.nodes.clear();
        serverThread.localNeighbors.flag=0;
    }

    private static void serialize(String filename, String nodesFilename, HashMap<Integer, HashSet<Integer>> G, HashSet<Integer> nodes) throws IOException {
        InputStreamReader isr = new InputStreamReader(
                new FileInputStream(filename));
        //new FileInputStream("data\\test.gml"));
        BufferedReader br = new BufferedReader(isr);

        String line;
        boolean isNode = true;
        int i = 0;
        while ((line = br.readLine()) != null) {
            Integer source = Integer.valueOf(line.trim().split(" ")[0]);
//            line = br.readLine();
            Integer target = Integer.valueOf(line.trim().split(" ")[1]);
            if (G.get(source) == null) {
                G.put(source, new HashSet<Integer>());
            }
            if (G.get(target) == null) {
                G.put(target, new HashSet<Integer>());
            }
            G.get(source).add(target);
            G.get(target).add(source);
        }
//        }
        //读入节点
        isr = new InputStreamReader(new FileInputStream(nodesFilename));
        br = new BufferedReader(isr);
        while ((line = br.readLine()) != null) {
            Integer source = Integer.valueOf(line.trim().split(" ")[0]);
            nodes.add(source);
        }

    }


    private static void colMAdmin(int givennode, ServerThread serverThread, HashMap<Integer, HashSet<Integer>> G, String filename) throws Exception { //主网络处理

        //2.初始化子网络的信息
        Integer ein = 0;
        Integer eout;
        if (G.containsKey(givennode)) {
            eout = G.get(givennode).size();
        } else {
            eout = 0;
        }
        ArrayList<Integer> C = new ArrayList();
        C.add(givennode);
        //System.out.println("initial"+ein+", out"+eout);
        //3.全局网络的初始化
        int bestnode = givennode;
        double bestM;
        double M = 0;
        //交互,得到的节点并集中的每个节点
        serverThread.candidateNodes.remove(givennode);
        while (True) {
            bestM = -1;  //bestM值置为-1,小于M值。避免没有节点，导致的无线循环。
//            if (serverThread.candidateNodes.size()==0){
//                System.out.println("foundComunities" + C);
//                writeCommunities(givennode, C, filename);
//                serverThread.broadcast(new End(1));
//                break;  //发送程序终止
//                 }
            for (Integer nodev : serverThread.candidateNodes) {
                //1. 发送给子网络一个节点id
                serverThread.broadcast(new EdgeReq(serverThread.getHandlerId(), nodev));
                //System.out.println("nodevindo"+nodev);
                //3. 计算将此节点与社区C后的内部边和外部边
                Integer[] temps = EinEout(nodev, ein, eout, C, G);

                while (true) {//5. 接收来自其他子网络的数据，
                    if (serverThread.EdgeRsps.size() >= (serverThread.clients().size() - 1)) {
                        break;
                    } else {
                        Thread.sleep(1000);
                    }
                }
                // 6. ein,eout分别求和，得到整个网络的内部边数及外部边数
                double sum_e_cCurrent = temps[0];//初始化为当前网络的值
                double sum_e_outCurrent = temps[1];  //初始化为当前网络的值
                //System.out.println("current"+sum_e_cCurrent+", out"+sum_e_outCurrent);
                for (EdgeRsp temprsp : serverThread.EdgeRsps) {
                    sum_e_cCurrent = sum_e_cCurrent + temprsp.getInFrag();
                    sum_e_outCurrent = sum_e_outCurrent + temprsp.getOutFrag();
                    //System.out.println("current"+sum_e_cCurrent+", out"+sum_e_outCurrent);
                }
                int sum_e_cCurrentI = (int) Math.round(sum_e_cCurrent);
                int sum_e_outCurrentI = (int) Math.round(sum_e_outCurrent);
                serverThread.EdgeRsps.clear();
//                System.out.println("sum_e_cCurrentI"+sum_e_cCurrentI);
//                System.out.println("sum_e_outCurrentI"+sum_e_outCurrentI);

                //7.对比当前的M值，比当前的M值大，则更新，
                Double tempM = (double) Math.round((1.0 * sum_e_cCurrentI / sum_e_outCurrentI) * 10000) / 10000;
                //System.out.println(nodev+":  tempM"+tempM+",bestM"+bestM);
                if (tempM > bestM || (tempM.equals(bestM) && nodev < bestnode)) {
                    bestM = tempM;
                    bestnode = nodev;
                }
            }

            if (M <= bestM) {  //8. 广播加入社区中的节点bestnode,,
                C.add(bestnode);
                M = bestM;
                System.out.println("bestnode"+bestnode);
                serverThread.candidateNodes.remove(bestnode);////将此节点从并集中删除
                serverThread.broadcast(new Best(bestnode)); //广播
                Integer[] temps = EinEout(bestnode, ein, eout, C, G);//更新当前子网络的ein, eout
                ein = temps[0];
                eout = temps[1];
                UnionExtend tempUnionflag = new UnionExtend(0);
                if (G.containsKey(bestnode)) {
                    //判断是否扩展并集
                    HashSet tempNeigh = (HashSet) G.get(bestnode).clone();
                    tempNeigh.removeAll(serverThread.candidateNodes); //邻居节点，除去已访问的节点和社区中的节点
                    tempNeigh.removeAll(C);

                    if (tempNeigh.size() >= 1) {//存在邻居不在并集中， tempUnionflag.flag置1
                        tempUnionflag.flag = 1;
                    }
                } else {
                    tempUnionflag.flag = 0;
                }

                while (true) { //5. 接收来自其他子网络的数据，
                    if (serverThread.unionFlags.size() >= (serverThread.clients().size() - 1)) {
                        break;
                    } else {
                        Thread.sleep(1000);
                    }
                }

                for (UnionExtend temp : serverThread.unionFlags) {
                    tempUnionflag.flag = tempUnionflag.flag + temp.flag;
                }
                //清除serverThread.unionFlags
                serverThread.unionFlags.clear();
                serverThread.broadcast(tempUnionflag);

                if (tempUnionflag.flag >= 1) {//如果需要计算，启动计算并集程序--要改

                    HashSet<Integer> tempneighbors;
                    if (G.containsKey(bestnode)) {
                        HashSet<Integer> k_neighbors = (HashSet<Integer>) G.get(bestnode).clone();
                        tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                        for (int j = 1; j < k; j++) {
                            for (Integer node : tempneighbors) {
                                k_neighbors.addAll(G.get(node));
                            }
                            tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                        }
                        tempneighbors.removeAll(C);
                    } else {
                        tempneighbors = new HashSet<Integer>();
                    }
                    tempneighbors.addAll(serverThread.candidateNodes);
                    //System.out.println("tempneighbors--2"+tempneighbors.toString());
                    ElGamalSafeUnionAdminNetwork(tempneighbors, nodesNum, serverThread); ///
                    serverThread.unionFlags.clear();
                }
            } else {
                System.out.println("foundComunities" + C);
                writeCommunities(givennode, C, filename);
                serverThread.broadcast(new End(1));
                break;  //发送程序终止
            }
        }
    }


    private static EdgeRsp safeSumDispersion(int node, int ein, int eout, ServerThread serverThread) throws IOException, InterruptedException {  //将内部边和外部边分散

        EdgeRsp rsp = new EdgeRsp(0.0, 0.0);//根据要计算的节点id获得计算的边数结果，把别人发送的边数也加上
        rsp.setInFrag(ein);//当前网络的内部边数
        rsp.setOutFrag(eout);//当前网络的外部边数

        return rsp;
    }


    private static void colMsub(Integer givennode, ServerThread serverThread, HashMap<Integer, HashSet<Integer>> G) throws Exception {//子网络处理

        //2.初始化子网络的信息
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
            if (serverThread.EdgeReqs.size() == 1) {
                Integer[] temps = EinEout(serverThread.EdgeReqs.get(0).nodeId, ein, eout, Ci, G);
                int e_cCurrent = temps[0];
                int e_outCurrent = temps[1];
                EdgeRsp rsp = safeSumDispersion(serverThread.EdgeReqs.get(0).nodeId, e_cCurrent, e_outCurrent, serverThread);//4. 将计算的内部边，外部边，分成随机数发送到其他子网络
                serverThread.clients().get("localhost:8100").send(rsp); //发送给协调节点
                serverThread.EdgeReqs.clear();
//                System.out.println("rsp"+rsp.getInFrag());
//                System.out.println("rsp"+rsp.getOutFrag());

            } else if (!(serverThread.best == null)) { //如果接收最佳节点
                //8. 接收加入社区的节点bestnode,更新ein, eout
                Integer bestnode = serverThread.best.best; //best.best
                serverThread.best = null;
                Ci.add(bestnode);
                serverThread.candidateNodes.remove(bestnode);
                Integer[] temps = EinEout(bestnode, ein, eout, Ci, G);
                ein = temps[0];
                eout = temps[1];
                UnionExtend tempUnionflag = new UnionExtend(0);
                if (G.containsKey(bestnode)) { //如果节点在自网络中，再进行如下处理
                    //判断是否需要扩展交集
                    HashSet tempNeigh = (HashSet) G.get(bestnode).clone();
                    //tempNeigh.removeAll(serverThread.nodes); //要删除社区内的节点，也要改
                    tempNeigh.removeAll(Ci);
                    tempNeigh.removeAll(serverThread.candidateNodes); //邻居节点，除去已访问的节点和社区中的节点

                    if (tempNeigh.size() >= 1) {//存在邻居不在并集中，发送给1
                        tempUnionflag.flag = 1;
                    }
                } else {

                    tempUnionflag.flag = 0;
                }
                serverThread.clients().get("localhost:8100").send(tempUnionflag);

                //接收来自admin的信息
                while (true) { //5. 接收来主网络的数据，标志是否向外扩展
                    if (serverThread.unionFlags.size() >= 1) {
                        break;
                    } else {
                        Thread.sleep(1000);
                    }
                }
                if (serverThread.unionFlags.get(0).flag >= 1) {
                    HashSet<Integer> tempneighbors;
                    if (G.containsKey(bestnode)) {
                        HashSet<Integer> k_neighbors = (HashSet<Integer>) G.get(bestnode).clone();//重新计算交集
                        tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                        for (int j = 1; j < k; j++) {
                            for (Integer node : tempneighbors) {
                                k_neighbors.addAll(G.get(node));
                            }
                            tempneighbors = (HashSet<Integer>) k_neighbors.clone();//深拷贝
                        }
                        tempneighbors.removeAll(Ci);
                        tempneighbors.addAll(serverThread.candidateNodes);//serverThread.nodes怎么是空的？
                        serverThread.nodes.clear();
                    } else {
                        tempneighbors = new HashSet<Integer>();
                    }

                    //System.out.println("tempneighbors--2"+tempneighbors.toString());
                    ElGamalSafeUnionSubNetwork(tempneighbors, nodesNum, serverThread);
                }
                serverThread.unionFlags.clear();

            } else if (serverThread.endFlag.flag.equals(1)) { //如果接收到end，break
                System.out.println("收到节点结束--消息" + Ci);
                synchronized (serverThread.endFlag) {
                    serverThread.endFlag = new End(0);
                }
                break;
            } else if (serverThread.endFlag.flag.equals(-1)) { //如果接收到end，break
                System.out.println("收到end消息");
                System.exit(0);

            } else {
                Thread.sleep(100);
            }

        }
    }


    private static Integer[] EinEout(int nodev, int ein, int eout, ArrayList C, HashMap<Integer, HashSet<Integer>> G) throws IOException {
        Integer[] edgenums;
        if (G.containsKey(nodev)) {
            int dv = G.get(nodev).size();
            HashSet temp = new HashSet(C);
            HashSet x = (HashSet) G.get(nodev).clone();
            x.retainAll(temp);
            int x1 = x.size();
            int y = dv - x1;
            int e_cCurrent = ein + x1;
            int e_outCurrent = eout - x1 + y;
            edgenums = new Integer[]{e_cCurrent, e_outCurrent};
            //System.out.println("nodev"+nodev+", size  "+x.size()+"dv  "+dv+"in  "+ein +"out  "+eout);
        } else {
            edgenums = new Integer[]{ein, eout};
        }
        return edgenums;
    }

    // Initial two graph
    public static void conGragh(String filename, int seed, double x, double y, double z,  HashMap<Integer, HashSet<Integer>> originalG) throws IOException {//将一个网络拆分成3个网络
        //x是存在于所有网络的边的比例，y是属于属于两个网络的边的比例，z是只属于一个网络的边的比例，x=0.1, y=0.2, z=0.1,每个自网络所有的边比例：x+2y(与两个网络都有交集)+z=0.6

        InputStreamReader isr = new InputStreamReader(
                new FileInputStream(Handler.WORK_DIR +"\\src\\main\\resources\\data\\"+ filename+"\\"+filename+".gml"));
        //new FileInputStream("data\\test.gml"));

        ArrayList<Integer[]> edge_data = new ArrayList<>();
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            if (line.contains(" id ")) {
                Integer id = Integer.valueOf(line.trim().split(" ")[1]);
                HashSet<Integer> adjacentSet = new HashSet<Integer>();
                originalG.put(id, adjacentSet);
            } else if (line.contains("source")) {
                Integer source = Integer.valueOf(line.trim().split(" ")[1]);
                line = br.readLine();
                Integer target = Integer.valueOf(line.trim().split(" ")[1]);
                originalG.get(source).add(target);
                originalG.get(target).add(source);
                Integer[] temps = {source, target};
                temps[0] = source;
                temps[1] = target;
                edge_data.add(temps);
            }
        }

        ArrayList<Integer[]> graghEdge1 = new ArrayList<>();
        ArrayList<Integer[]> graghEdge2 = new ArrayList<>();
        ArrayList<Integer[]> graghEdge3 = new ArrayList<>();

        Random r1 = new Random();
        r1.setSeed(seed);

        double len=(double)edge_data.size();
        while ( (graghEdge1.size()/len)<= x ){
            double rand1 = r1.nextDouble();
            int indexN=(int)(rand1*(edge_data.size()));
            if( graghEdge1.contains(edge_data.get(indexN))){
                continue;
            }
            else{
                graghEdge1.add(edge_data.get(indexN));
            }
            //graghEdge1.add(edge_data.get(indexN));
        }

        while ( (graghEdge2.size()/len)<= y ){
            double rand1 = r1.nextDouble();
            int indexN=(int)(rand1*(edge_data.size()));
            //graghEdge2.add(edge_data.get(indexN));
            if( graghEdge2.contains(edge_data.get(indexN))){
                continue;
            }
            else{
                graghEdge2.add(edge_data.get(indexN));
            }
        }

        for (int i = 0; i < edge_data.size(); i++) {
            if ( !graghEdge1.contains(edge_data.get(i)) & !graghEdge2.contains(edge_data.get(i))){
                graghEdge3.add(edge_data.get(i));
            }
        }

        while ( (graghEdge3.size()/len)<= z ){
            double rand1 = r1.nextDouble();
            int indexN=(int)(rand1*(edge_data.size()));
            //graghEdge3.add(edge_data.get(indexN));
            if( graghEdge3.contains(edge_data.get(indexN))){
                continue;
            }
            else{
                graghEdge3.add(edge_data.get(indexN));
            }
        }

        // Assign edges to two networks, each of which must exist in at least one network
        //for (int i = 0; i < edge_data.size(); i++) {
//            double rand1 = r1.nextDouble();
//            if (rand1 <= x) {
//                graghEdge1.add(edge_data.get(i));
//                graghEdge2.add(edge_data.get(i));
//                graghEdge3.add(edge_data.get(i));
//            } else if (rand1 > x && rand1 <= x + y) {
//                graghEdge1.add(edge_data.get(i));
//                graghEdge2.add(edge_data.get(i));
//            } else if (rand1 > x + y && rand1 <= x + 2 * y) {
//                graghEdge1.add(edge_data.get(i));
//                graghEdge3.add(edge_data.get(i));
//            } else if (rand1 > x + 2 * y && rand1 <= x + 3 * y) {
//                graghEdge2.add(edge_data.get(i));
//                graghEdge3.add(edge_data.get(i));
//            } else if (rand1 > x + 3 * y && rand1 <= x + 3 * y + z) {
//                graghEdge1.add(edge_data.get(i));
//            } else if (rand1 > x + 3 * y + z && rand1 <= x + 3 * y + 2 * z) {
//                graghEdge2.add(edge_data.get(i));
//            } else {
//                graghEdge3.add(edge_data.get(i));
//            }
            //每个网络，一个随机数，x1,x2,x3
//            double x1=x;
//            double x2=y;
//            double rand1 = r1.nextDouble();  //网络1的随机数
//            if (rand1 <= x1) {
//                graghEdge1.add(edge_data.get(i));
//            }
//            double rand2 = r1.nextDouble();
//            if (rand2 <= x2) {  //网络2的随机数
//                graghEdge2.add(edge_data.get(i));
//            }
//            if ((rand1>x1) & (rand2>x2)){  //如果改边，既没有放入网络1，也没有放入网络2，则改边加入网络3
//                graghEdge3.add(edge_data.get(i));
//            }
//
//
//        }
//        for (int i = 0; i < edge_data.size(); i++) {
//            double x3 = z -(double) (graghEdge3.size()/edge_data.size());
//            double rand3 = r1.nextDouble();
//            if (rand3 <= x3) {  //网络3中的边还不够密度比例，继续往网络3中加边。
//                graghEdge3.add(edge_data.get(i));
//            }
//        }


        File tempfile = new File(Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+filename+"\\"+ String.valueOf(x)+ String.valueOf(y)+String.valueOf(z)+"\\"+seed+"\\");
        if(!tempfile.exists()){
            tempfile.mkdirs();
        }

        //写入文件，一个图的边写入一个文件 filename+graphName+"\\"+graphName+"\\.gml"
        File file = new File(Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+ filename+"\\"+ String.valueOf(x)+ String.valueOf(y)+String.valueOf(z)+"\\"+seed+ "\\G1.txt");
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        PrintStream oos = new PrintStream(fos);
        for (Integer[] edge : graghEdge1) {
            oos.println(edge[0] + " " + edge[1]);
            //System.out.println(edge[0]+" "+edge[1]+"\n");
        }
        oos.flush();
        oos.close();
        fos.close();
        System.out.println("G1");

        //写入文件，一个图的边写入一个文件
        file = new File(Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+filename+"\\"+ String.valueOf(x)+ String.valueOf(y)+String.valueOf(z)+"\\"+seed+"\\"+ "\\G2.txt");
        file.createNewFile();
        fos = new FileOutputStream(file);
        oos = new PrintStream(fos);
        for (Integer[] edge : graghEdge2) {
            oos.println(edge[0] + " " + edge[1]);
            //System.out.println(edge[0]+" "+edge[1]+"\n");
        }
        oos.flush();
        oos.close();
        fos.close();
        System.out.println("over");

        //写入文件，一个图的边写入一个文件
        file = new File(Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+ filename+"\\"+ String.valueOf(x)+ String.valueOf(y)+String.valueOf(z)+"\\"+seed+"\\"+ "\\G3.txt");
        file.createNewFile();
        fos = new FileOutputStream(file);
        oos = new PrintStream(fos);
        for (Integer[] edge : graghEdge3) {
            oos.println(edge[0] + " " + edge[1]);
            //System.out.println(edge[0]+" "+edge[1]+"\n");
        }
        oos.flush();
        oos.close();
        fos.close();
        System.out.println("over");

        //将节点写入文件
        file = new File(Handler.WORK_DIR + "\\src\\main\\resources\\data\\"+ filename+"\\"+ String.valueOf(x)+ String.valueOf(y)+String.valueOf(z)+"\\"+seed+"\\"+ "\\nodes.txt");
        file.createNewFile();
        fos = new FileOutputStream(file);
        oos = new PrintStream(fos);
        for (Integer node : originalG.keySet()) {
            oos.println(node );
            //System.out.println(edge[0]+" "+edge[1]+"\n");
        }
        oos.flush();
        oos.close();
        fos.close();
        System.out.println("over");


    }


    private static void writeCommunities(Integer givennode, ArrayList<Integer> C, String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(file, true);
        PrintStream oos = new PrintStream(fos);
        oos.print(givennode + ":");
        for (Integer node : C) {
            oos.print(node + " ");
        }
        oos.print("\n");
        oos.flush();
        oos.close();
    }


}
