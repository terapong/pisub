package pisub;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class SimpleMqttClient implements MqttCallback {
	private MqttClient myClient;
	private MqttConnectOptions connOpt;
	private static final int qos = 0;
	
	private static int numHouse;

	//static final String BROKER_URL = "tcp://192.168.1.53:1883";
	static final String BROKER_URL = "tcp://localhost:1883";
	static final String M2MIO_THING = "terapong_broker";
	
	static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
	static final String MYSQL_URL = "jdbc:mysql://localhost:3306/pi3?autoReconnect=true&useSSL=false";
	//static final String MYSQL_URL = "jdbc:mysql://192.168.1.53:3306/pi3?autoReconnect=true&useSSL=false";
	static final String MYSQL_USER = "toto";
	static final String MYSQL_PASS = "xxxxxx";
	
	static final String OPEN_PIN = "OPEN_PIN";
	static final String CLOSE_PIN = "CLOSE_PIN";
	
	static final String OPEN_POUT = "OPEN_POUT";
	static final String CLOSE_POUT = "CLOSE_POUT";
	
	static final String OPEN_FIN = "OPEN_FIN";
	static final String CLOSE_FIN = "CLOSE_FIN";
	
	static final String OPEN_FOUT = "OPEN_FOUT";
	static final String CLOSE_FOUT = "CLOSE_FOUT";
	
	static final String OPEN_FREL = "OPEN_FREL";
	static final String CLOSE_FREL = "CLOSE_FREL";
	
	static final String ALERT = "ALERT";
	static final String ALERT_OFF = "ALERT_OFF";
	
	static final String CLOSE_ALL = "CLOSE_ALL";
	static final String OPEN_ALL = "OPEN_ALL";
	
	public SimpleMqttClient() {
		super();
		connectToMQTT();
		try {
			initSystem();
			Thread.sleep(10000);
			runClient();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private void connectToMQTT() {
		// setup MQTT Client
		connOpt = new MqttConnectOptions();
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
	
		// Connect to Broker
		try {
			myClient = new MqttClient(BROKER_URL, M2MIO_THING);
			myClient.setCallback(this);
			myClient.connect(connOpt);
			//System.out.print("Attempting MQTT connection " + BROKER_URL + " ... ");
			if(myClient.isConnected()) {
				//System.out.println("Connected");
			} else {
				//System.out.println("Failed");
			}
		} catch (MqttException e) {
//			try {
//				Thread.sleep(5000);
//				System.out.println("Attempting MQTT connection " + BROKER_URL + " ... Failed");
//				connectToMQTT();
//			} catch(Exception ex) {
////				e.printStackTrace();
////				System.exit(-1);
//			}
		}
	}
	
	private void clearMessageHistory() throws Exception {
		Class.forName(MYSQL_DRIVER);
		Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS);
		String sql = "delete from messagehistory where id not in (select r.id from (select r1.id from messagehistory r1 order by r1.id desc limit 1000) as r )";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.executeUpdate();
		pstmt.close();
		conn.close();
	}
	
	public boolean isNumHouseChange() throws Exception {
		Class.forName(MYSQL_DRIVER);
		Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS);
		String  sql = "select count(id) as totle from house";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		ResultSet rs = pstmt.executeQuery();
		rs.next();
		int current = rs.getInt("totle");
		rs.close();
		pstmt.close();
		conn.close();
		if(current != numHouse) {
			numHouse = current;
			return true;
		} else {
			return false;
		}
	}
	
	private void initNode(long node_id) throws Exception {
		Class.forName(MYSQL_DRIVER);
		Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS);
		String  sql = "update house set isopenpout = false, isopenfin = false, isopenfout = false, isopenpin = false, isopenfrel = false, isopenalert = false where nodemcu_id = " + node_id;
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.executeUpdate();
		pstmt.close();
		conn.close();
	}
	
	private void initSystem() throws Exception {
		Class.forName(MYSQL_DRIVER);
		Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS);
		String  sql = "update house set isopenpout = false, isopenfin = false, isopenfout = false, isopenpin = false, isopenfrel = false, isopenalert = false";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		numHouse = pstmt.executeUpdate();

		MqttMessage pubMessage = new MqttMessage(CLOSE_ALL.getBytes());
		pubMessage.setQos(qos);
		pubMessage.setRetained(false);
		MqttDeliveryToken token = null;
		
		sql = "select * from nodemcu";
		pstmt = conn.prepareStatement(sql);
		ResultSet rs = pstmt.executeQuery();
		
		while(rs.next()) {
			String myTopic = String.valueOf(rs.getLong("id")) + "/pi3/sub";
			MqttTopic topicControl = myClient.getTopic(myTopic);
			// publish message to broker
			//System.out.println("Publishing to topic \"" + myTopic + "\" qos " + qos + " Message " + CLOSE_ALL);
			token = topicControl.publish(pubMessage);
			// Wait until the message has been delivered to the broker
			token.waitForCompletion();
			Thread.sleep(1000);
		}
		
		rs.close();
		pstmt.close();
		conn.close();
	}

	@Override
	public void connectionLost(Throwable t) {
		while(!myClient.isConnected()) {
			try {
				Thread.sleep(5000);
				//System.out.println("Connection lost!");
				connectToMQTT();
				runClient();
			} catch(Exception ex) {
//				e.printStackTrace();
//				System.exit(-1);
			}
		}
	} 

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		try {
			//System.out.println("ส่ง ข้อความเรียบร้อย : ");
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override	
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		//delete messageHistory accept last 1,000 row
		clearMessageHistory();
		//System.out.println(topic);
		if(topic.indexOf("/connected") != -1) {
			//System.out.println("Connected Topic : " + topic);
			try {
				initNode(Long.valueOf(topic.split("/")[0]));
				Thread.sleep(1000);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		} else {
			(new Thread(new ManageTopic(topic, message, myClient))).start();
		}
	}
	
	public void runClient() {
		String myTopic;
		Long id;
		try {
			Class.forName(MYSQL_DRIVER);
			Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS);
			
			String sql = "select * from topic where nodemcu_id in (select nodemcu_id from house)";
			PreparedStatement pstmt = conn.prepareStatement(sql);
			ResultSet rs = pstmt.executeQuery();
			
			while(rs.next()) {
				myTopic = rs.getString("name");
				id = rs.getLong("nodemcu_id");
				if(myTopic.indexOf("sub") == -1) {
					//System.out.println(myTopic);
					myClient.subscribe(myTopic, qos);
					Thread.sleep(500);
				}
				myClient.subscribe(id + "/pi3/connected", qos);
				Thread.sleep(500);
			}
			
			
			
			rs.close();
			pstmt.close();
			conn.close();
		} catch(Exception ex) {
			//ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		SimpleMqttClient smc = new SimpleMqttClient();
		
		while (true) {
			Thread.sleep(10000);
			if(smc.isNumHouseChange()) {
				//System.out.println("Add or Delete House Event");
				smc.runClient();
			}
		}
	}
}