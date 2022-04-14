package multinetwork.base;

import lombok.Getter;
import multinetwork.msg.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ServerThread extends Thread {
    @Getter
    public final String handlerId;//IP+端口号
    private final ServerSocket serverSocket;//接收其他机器连接
    public final HashMap<String, ClientThread> handlers = new HashMap<>();//所有的连接客户端或者服务器，key：IP+端口，value：连接

    public ArrayList<EdgeReq> EdgeReqs = new ArrayList<>();
    public ArrayList<EdgeRsp> EdgeRsps = new ArrayList<>();

    public HashSet<Integer> nodes = new HashSet<>();
    public HashSet<Integer> candidateNodes = new HashSet<>(); //邻居节点（或者多层邻居），对应于N
    public ArrayList<UnionExtend> unionFlags = new ArrayList<>(); //是否扩展的flag

    public LocalNeighbors localNeighbors = new LocalNeighbors(new HashSet<Integer>());

    public Best best = null;//记录选择的节点
    public Object bestLock = new Object();
    public End endFlag = new End(0);//记录选择的节点

    private static boolean True;


    public ServerThread(String handlerId) throws IOException {
        this.handlerId = handlerId;
        this.serverSocket = new ServerSocket(Integer.parseInt(handlerId.substring(handlerId.indexOf(":") + 1)));
    }

    @Override
    public void run() {  //监听与其他客户端的连接
        while (!isInterrupted()) {
            try {
                Socket socket = serverSocket.accept();//接收其他客户端的连接，没有连接则阻塞
                ClientThread clientThread = new ClientThread(ServerThread.this, socket);//新建客户端，包含对方的Ip+端口号
                synchronized (handlers) {//对hash加锁
                    handlers.put(clientThread.getHandlerId(), clientThread);//把客户端放到hashmap中
                }

                clientThread.send(new HandlerId(handlerId));//把自己的id发送给客户端
                clientThread.send(handlerList());//自己已连接的客户端发给新客户端
                clientThread.start();//客户端启动监听新客户端的消息
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public HashMap<String, ClientThread> clients() {
        return handlers;
    }//获取所有客户端

    private HandlerList handlerList() { //获取自己所有客户端的id，取handlers中的key
        HandlerList handlerList = new HandlerList();
        synchronized (handlers) {
            for (String node : handlers.keySet()) {
                handlerList.getHandlerList().add(node);
            }
        }
        return handlerList;
    }

    public void connect(String address) throws IOException {
        synchronized (handlers) {
            if (!handlers.containsKey(address)) {//判断连接是否已经存在
                ClientThread clientThread = new ClientThread(ServerThread.this, new Socket(address.substring(0, address.indexOf(":")), Integer.parseInt(address.substring(address.indexOf(":") + 1))));
                //此客户端对应的服务端（自己），IP，端口号
                clientThread.start();//监听新客户端的客户端
                handlers.put(clientThread.getHandlerId(), clientThread);//存放新的客户端的Ip，端口号，监听此客户端的客户端
                broadcast(handlerList());
                System.out.println("连接成功：" + address);
            }
        }
    }

    public void broadcast(Object obj) {
        synchronized (handlers) {
            Iterator<Map.Entry<String, ClientThread>> iterator = handlers.entrySet().iterator();//遍历客户端集合
            while (iterator.hasNext()) {
                Map.Entry<String, ClientThread> entry = iterator.next(); //对于每个客户端
                try {
                    if (!(entry.getKey().equals(handlerId))) { //不给自己发广播
                        entry.getValue().send(obj); //发送消息
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    iterator.remove();
                }
            }
        }
    }


}
