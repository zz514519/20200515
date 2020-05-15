package udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//重构第一步：把收到的请求内容，作为响应直接发送回去------回显服务
//重构第二步：多加一些调试打印信息，便于观察发生了什么
//

//Server必须公开端口，否则客户端找不到
//端口（port）可以在0···65535之间随便选
//但是不能使用已经被其他进程使用的端口 -- 端口只能属于唯一的一个进程
public class Server {
    static final int PORT = 9521;
    static final String CHARSET = "UTF-8";

    //Map<英文单词，中文含义>
    private static final Map<String,String> meaningMap= new HashMap<>();
    //Map<英文单词，实例语句>
    private static final Map<String,List<String>> exampleSentencesMap = new HashMap<>();

    static {
        //在静态代码块中对两个map进行初始化
        meaningMap.put("give","vt. 给；产生；让步；举办；授予");
        exampleSentencesMap.put("give",new ArrayList<>());
        exampleSentencesMap.get("give").add("hello");
    }

    public static void main(String[] args)throws IOException {
        //1.创建套接字
        //DatagramSocket 是UDP协议专用的套接字
        //PORT是我选好的准备开饭店的地址

        System.out.println("DEBUG：准备开一家饭店");
        try(DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            System.out.printf("DEBUG: 在 %d 这个端口上开好一家饭店了%n", PORT);

            //提前准备好一个字节数组用来存放接收到的数据（请求）
            //一次最多可以接收8132个字节

            byte[] receiveBuffer = new byte[8192];

            while (true){
                System.out.println("=====================================");
                //一次循环就是接收一次请求--响应处理过程
                //1.接收对方发过来的请求（数据）
                //1.1必须先创建DatagramPackrt数据报文对象

                DatagramPacket packetFromClient = new DatagramPacket(receiveBuffer,0,receiveBuffer.length);
                System.out.println("DEBUG：准备好了接收用的packet");
                //1.2接收数据
                serverSocket.receive(packetFromClient);         //这个方法不是立即返回的，和scanner。nextLine();对比
                System.out.println("DEBUG：真正收到了客户端发来的数据");
                //当走到这里时，数据一定接收到了
                //packetFromClient.getLength();一个收到了多少字节的数据

                //1.3因为我们收到的是字节流格式数据，所以我们把数据解码成字符格式
                //      需要字符集编码的知识
                //   利用String的一个构造方法，把字节数组的数据解码成字符格式的数据
                String request = new String(receiveBuffer,0,packetFromClient.getLength(),CHARSET);
                System.out.println("DEBUG: 收到的请求是："+request);

                //1.4我们跳过了理解请求的这一步-----我们没有设计应用层协议

                //1.4.1 请求是英文单词
                //      根据英文单词获取含义 + 示例语句
                //      需要考虑，用户属于的请求不是我们支持的单词

                String response = "没有这个单词";
                String template = "含义：\r\n%s\r\n示例语句:%s\r\n";
                String exampleTemplate = "%d. %s\r\n";
                if(!meaningMap.containsKey(request)){
                    String meaning = meaningMap.get(request);
                    List<String> sentenceList = exampleSentencesMap.get(request);
                    StringBuilder exampleSB = new StringBuilder();
                    for (int i = 0; i < sentenceList.size(); i++) {
                        exampleSB.append(String.format(exampleTemplate,i+1,sentenceList.get(i)));
                    }
                    response  = String.format(template,meaning,exampleSB.toString());
                }

                //1.5业务处理请求
                //String response = request;

                //1.6发送响应
                //如何获取客户端进程的ip + port
                InetAddress clientAddress = packetFromClient.getAddress();
                int clientPort = packetFromClient.getPort();
                System.out.printf("DEBUG:：客户端的唯一标识是(%s:%d)%n",clientAddress.getHostAddress(),clientPort);

                byte[] responseBytes = response.getBytes(Server.CHARSET);
                DatagramPacket packetToClient = new DatagramPacket(
                        responseBytes, 0, responseBytes.length,         //要发送的数据
                        clientAddress,clientPort);          //要发送给端进程

                System.out.println("DEBUG：准备好了发送用的packet");
                serverSocket.send(packetToClient);
                System.out.println("DEBUG: 成功把响应发送给客户端");
                System.out.println("===================================");
            }
        }
    }
}