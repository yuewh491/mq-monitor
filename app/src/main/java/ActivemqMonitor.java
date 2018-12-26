import com.taobao.diamond.client.impl.DiamondClientFactory;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by yueweihang on 2016/4/1.
 * 配置文件位置：${ActiveMQ.Base}/conf/activemq.xml
 * 将broker节点添加属性useJmx=true
 *  <broker xmlns="http://activemq.apache.org/schema/core" brokerName="localhost" dataDirectory="${activemq.data}" useJmx="true">
     <managementContext>
     <managementContext createConnector="true" connectorPort="2011" jmxDomainName="pay-broker"/>
     </managementContext>
    5.8以前

    5.8版本以后
     <broker xmlns="http://activemq.apache.org/schema/core" brokerName="localhost" dataDirectory="${activemq.data}" useJmx="true">
     <managementContext>
     <managementContext createConnector="true"/>
     </managementContext>
 通过URL：service:jmx:rmi:///jndi/rmi://localhost:2011/jmxrmi初始化JMXConnector
 通过NAME：my-broker:BrokerName=localhost,Type=Broker获取BrokerViewMBean，可以获取Broker的相关属性，可以通过BrokerViewMBean获取QueueViewMBean及TopicViewMBean等，从而得到Queue及Topic的属性
 */

public class ActivemqMonitor {
    private final static String MESSAGE_URL = "http://internal.pay.taoche.com/mns-web/services/rest/msgNotify";
    private static Logger logger = LogManager.getLogger("App");
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        logger.info("################ActivemqMonitor程序执行开始 " + sdf.format(new Date()) + "################");
        JMXConnector connector = null;
        try {
            String threshold = DiamondClientFactory.getSingletonDiamondSubscriber().getAvailableConfigureInfomation("MONITOR_THRESHOLD", "ACTIVEMQ_MONITOR", 1000);
            //String jmxUrl = DiamondClientFactory.getSingletonDiamondSubscriber().getAvailableConfigureInfomation("MONITOR_JMX", "ACTIVEMQ_MONITOR", 1000);
            String jmxUrl = "service:jmx:rmi:///jndi/rmi://192.168.155.163:2011/jmxrmi";
            logger.info("threshold：{}", threshold);
            logger.info("jmxUrl：{}", jmxUrl);

            JMXServiceURL url = new JMXServiceURL(jmxUrl);
            connector = JMXConnectorFactory.connect(url, null);
            connector.connect();
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            // 需要注意的是，这里的my-broker必须和上面配置的名称相同
            //ObjectName name = new ObjectName("pay-broker:BrokerName=localhost,Type=Broker");
            ObjectName name = new ObjectName("org.apache.activemq:type=Broker,brokerName=localhost");//activemq5.8以上
            BrokerViewMBean mBean = (BrokerViewMBean) MBeanServerInvocationHandler.newProxyInstance(connection, name, BrokerViewMBean.class, true);

            for (ObjectName queueName : mBean.getQueues()) {
                QueueViewMBean queueMBean = (QueueViewMBean) MBeanServerInvocationHandler.newProxyInstance(connection, queueName, QueueViewMBean.class, true);
                // 消息队列名称
                String qname = queueMBean.getName();
                // 队列中剩余的消息数
                long pendingMessages = queueMBean.getQueueSize();
                // 消费者数
                //queueMBean.getConsumerCount();
                // 出队数
                //queueMBean.getDequeueCount() ;
                String message = String.format("队列：%s已经积压%s条消息！", qname, pendingMessages);
                logger.info(message);
                if (pendingMessages > Integer.parseInt(threshold)) {
                    String users = DiamondClientFactory.getSingletonDiamondSubscriber().getAvailableConfigureInfomation("MONITOR_USER", "ACTIVEMQ_MONITOR", 1000);
//                    //String requestParameters = "?_appid=mall&mobile=" + users + "&message=" + message;
//                    //HttpUtil.get(MESSAGE_URL, requestParameters);
//
//                    for (String user : users.split(",")) {
//                        sendMessage(user, message);
//                    }
                }
            }

        } catch (MalformedURLException ex) {
            logger.error("MalformedURLException原因:" + ex);
        } catch (IOException ex) {
            logger.error("IOException原因:" + ex);
        } catch (MalformedObjectNameException ex) {
            logger.error("MalformedObjectNameException原因:" + ex);
        } catch (Exception ex) {
            logger.error("Exception原因:" + ex);
        } finally {
            try {
                if (connector != null) {
                    connector.close();
                }
            } catch (Exception ex) {
                logger.error("connector关闭失败:" + ex);
            }
        }
        logger.info("################ActivemqMonitor程序执行结束" + sdf.format(new Date()) + "################");
        System.exit(0);
    }


    private static void sendMessage(String phoneNum, String content) {
        Map<String, String> map = new HashMap<>();
        map.put("appId", "activemq");
        map.put("orderNo", UUID.randomUUID().toString().replaceAll("[-]", ""));
        map.put("isRealTime", "true");
        map.put("targetCount", "1");
        map.put("targetIdenty", phoneNum);
        map.put("protocol", "S");
        map.put("content", content);

        try {
            String result = HttpUtil.post(MESSAGE_URL, map);
        } catch (Exception e) {
            logger.error("短信发送失败:{}", e);
        }
    }
}