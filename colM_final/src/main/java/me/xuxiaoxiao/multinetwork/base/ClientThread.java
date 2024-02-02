package me.xuxiaoxiao.multinetwork.base;

import lombok.Getter;
import me.xuxiaoxiao.multinetwork.Handler;
import me.xuxiaoxiao.multinetwork.msg.*;
import org.bouncycastle.jce.interfaces.ElGamalPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.annotation.Nonnull;
import javax.crypto.spec.DHParameterSpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidParameterSpecException;
import java.util.*;

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

    //加密需要：非对称密钥算法
    public static final String KEY_ALGORITHM = "ElGamal";
    /**
     * 密钥长度，DH算法的默认密钥长度是1024
     * 密钥长度必须是8的倍数，在160到16384位之间
     */
    private static final int KEY_SIZE = 160;
    private static final String PUBLIC_KEY = "ElGamalPublicKey"; //公钥
    private static final String PRIVATE_KEY = "ElGamalPrivateKey"; //私钥
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
                }else if (clazz.equals(EdgessReq.class)) { //边请求消息
                    synchronized (serverThread.EdgessReqs) {
                        serverThread.EdgessReqs.add((EdgessReq) Handler.GSON.fromJson(content, clazz));
                    }
                }
                else if (clazz.equals(EdgeFragment.class)) { //接收到碎片信息
                    synchronized (serverThread.EdgeFragments) {
                        serverThread.EdgeFragments.add( (EdgeFragment) Handler.GSON.fromJson(content, clazz)); //提取出边信息
                    }
                } else if (clazz.equals(PrimeGen.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.pandg) {
                        serverThread.pandg=((PrimeGen) Handler.GSON.fromJson(content, clazz));
                        System.out.println("received serverThread.Pandg"+serverThread.pandg.p.toString());
                    }
                } else if (clazz.equals(Hi.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.his) {
                        System.out.println("serverThread.his要加锁，因为要接收新的值");
                        serverThread.his.add((Hi) Handler.GSON.fromJson(content, clazz));
                        System.out.println(" client received "+ serverThread.his.size());}
                } else if (clazz.equals(UV.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.uv) {
                        serverThread.uv=(UV) Handler.GSON.fromJson(content, clazz);}
                } else if (clazz.equals(W.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.ws) {
                        serverThread.ws.add((W) Handler.GSON.fromJson(content, clazz));
                    }

                } else if (clazz.equals(HashSet.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.nodes) {
                        HashSet<Double>   temps= (HashSet<Double>) Handler.GSON.fromJson(content, clazz);
                        HashSet<Integer>   tempsNodes = new HashSet<>();
                        for (Double temp :temps){
                            tempsNodes.add( new Integer(temp.intValue()));
                        }
                        serverThread.nodes=tempsNodes;
                    }
                } else if (clazz.equals(Best.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.bestLock) {
                        serverThread.best= (Best) ( Handler.GSON.fromJson(content, clazz));
                    }

                } else if (clazz.equals(Bests.class)) { //所有s服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.bestLock) {
                        serverThread.bests = (Bests) (Handler.GSON.fromJson(content, clazz));
                    }

                }else if (clazz.equals(End.class)) { //所有服务器生，所有边数再次累加，针对管理机器
                    synchronized (serverThread.endFlag) {
                        serverThread.endFlag= (End) Handler.GSON.fromJson(content, clazz);
                        System.out.println("serverThread.endFlag"+serverThread.endFlag);
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
