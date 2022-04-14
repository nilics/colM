package multinetwork.base;

import lombok.Getter;
import multinetwork.Handler;
import multinetwork.msg.*;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class ClientThread extends Thread {
    private final ServerThread serverThread;//自己的客户端对应的服务端对象
    private final Socket socket; //socket连接，用于接收和发送消息的
    private final BufferedInputStream inStream;
    private final BufferedOutputStream outStream;
    private final Scanner scanner;
    private final PrintStream printStream;
    @Getter
    public String handlerId;  //当前客户端监听的节点（服务器或者客户端）

    public String infos = new String();
    private static boolean True;


    private static final HashMap<String, EdgeRsp> rsps = new HashMap<>(); //边计算结果

    public ClientThread(ServerThread serverThread, Socket socket) throws IOException {
        this.serverThread = serverThread;
        this.socket = socket;
        this.inStream = new BufferedInputStream(socket.getInputStream());
        this.outStream = new BufferedOutputStream(socket.getOutputStream());
        this.scanner = new Scanner(this.inStream);
        this.printStream = new PrintStream(this.outStream);

        send(new HandlerId(serverThread.getHandlerId()));  //自己的服务端id发送给别人

        String idFrom = scanner.nextLine(); //读其他客户端发过来的消息，从中获取对方的ip和端口号
        HandlerId handlerId = Handler.GSON.fromJson(idFrom.substring(idFrom.indexOf(":") + 1), HandlerId.class);
        this.handlerId = handlerId.getId();
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) { //循环，一直监听消息
                String msg = scanner.nextLine(); //读
                //System.out.println("接收到消息：" + msg);
                Class<?> clazz = Class.forName(msg.substring(0, msg.indexOf(":")));// 类型+消息，冒号分隔，获取类型，由字符串转换为类型
                String content = msg.substring(msg.indexOf(":") + 1); //获取消息
                if (clazz.equals(HandlerList.class)) { //如果是HandlerList类型
                    Object obj = Handler.GSON.fromJson(content, clazz);
                    for (String node : ((HandlerList) obj).getHandlerList()) {
                        try {
                            serverThread.connect(node); //对HandlerList中的对象进行连接
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (clazz.equals(EdgeReq.class)) { //边请求消息

                    synchronized (serverThread.EdgeReqs) {
                        serverThread.EdgeReqs.add((EdgeReq) Handler.GSON.fromJson(content, clazz));
                    }
                } else if (clazz.equals(LocalNeighbors.class)) { //所有服务器生，邻居节点
                    synchronized (serverThread.localNeighbors) {
                        serverThread.localNeighbors = (LocalNeighbors) Handler.GSON.fromJson(content, clazz);
                        serverThread.localNeighbors.flag=1;
                    }
                } else if (clazz.equals(HashSet.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.nodes) {
                        HashSet<Double> temps = (HashSet<Double>) Handler.GSON.fromJson(content, clazz);
                        HashSet<Integer> tempsNodes = new HashSet<>();
                        for (Double temp : temps) {
                            tempsNodes.add(new Integer(temp.intValue()));
                        }
                        serverThread.nodes = tempsNodes;
                    }
                } else if (clazz.equals(Best.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.bestLock) {
                        serverThread.best = (Best) (Handler.GSON.fromJson(content, clazz));
                    }

                } else if (clazz.equals(End.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.endFlag) {
                        serverThread.endFlag = (End) Handler.GSON.fromJson(content, clazz);
                        System.out.println("serverThread.endFlag" + serverThread.endFlag);
                    }

                } else if (clazz.equals(UnionExtend.class)) { //是否扩展并集
                    synchronized (serverThread.unionFlags) {
                        serverThread.unionFlags.add((UnionExtend) Handler.GSON.fromJson(content, clazz));
                    }

                } else if (clazz.equals(EdgeRsp.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.EdgeRsps) {
                        serverThread.EdgeRsps.add((EdgeRsp) Handler.GSON.fromJson(content, clazz));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final void send(@Nonnull Object obj) {
        //System.out.println("发送：" + Handler.GSON.toJson(obj));
        printStream.println(obj.getClass().getName() + ":" + Handler.GSON.toJson(obj));
        printStream.flush();
    }


}
