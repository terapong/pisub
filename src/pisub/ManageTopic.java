package pisub;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class ManageTopic implements Runnable {
	private MqttClient myClient;
	private String topic;
	private MqttMessage message;
	private int qos = 0;
	
	static final boolean OPEN = true;
	static final boolean CLOSE = false;

	static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
	//static final String MYSQL_URL = "jdbc:mysql://192.168.1.53:3306/pi3?autoReconnect=true&useSSL=false";
	static final String MYSQL_URL = "jdbc:mysql://localhost:3306/pi3?autoReconnect=true&useSSL=false";
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
	
	public ManageTopic(String topic, MqttMessage message, MqttClient myClient) {
		this.topic = topic;
		this.message = message;
		this.myClient = myClient;
	}

	private void sendControl(String pubMsg) {
		// setup MQTT Client
		String subTopic = topic.substring(0, topic.indexOf("i3") + 3) + "sub";
	
		// Connect to Broker
		MqttMessage pubMessage = new MqttMessage(pubMsg.getBytes());
		pubMessage.setQos(qos);
		pubMessage.setRetained(false);
		
		//System.out.println("Publishing to topic \"" + subTopic + "\" qos " + qos + " Message " + pubMsg);
		MqttDeliveryToken token = null;
		MqttTopic topicControl = myClient.getTopic(subTopic);
		try {
			// publish message to broker
			token = topicControl.publish(pubMessage);
			// Wait until the message has been delivered to the broker
			token.waitForCompletion();
			Thread.sleep(100);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private int ifInTempGtOutTemp(Connection conn, BigDecimal inTemp) throws Exception {
		Message ms = new Message();
		String sql = "select *, t.id, t.name from message m " + 
					"left outer join topic t on m.topic_id = t.id " + 
					"where t.name = '" + topic.substring(0, topic.indexOf("in")) + "out" + "'";
		//System.out.println(sql);
		PreparedStatement pstm = conn.prepareStatement(sql);
		ResultSet rs = pstm.executeQuery();
		if(rs.next()) {
			ms.setId(rs.getLong("id"));
			ms.setMessage(rs.getBigDecimal("message"));
			ms.setRgb(rs.getString("rgb"));
		}
		
		try {
			if(inTemp.compareTo(ms.getMessage()) > 0) {
				return 1;
			} else {
				return -1;
			}
		} catch(Exception ex) {
			return 0;
		}
	}
	
	private House getHouseById(Connection conn, long id) throws Exception {
		House h = new House();
		String sql = "select * from house where id = " + id;
		PreparedStatement pstmt = conn.prepareStatement(sql);
		ResultSet rs = pstmt.executeQuery();
		rs.next();
		h.setId(rs.getLong("id"));
		h.setName(rs.getString("name"));
		
		h.setOpenPout(rs.getBoolean("isopenpout"));
		h.setOpenFin(rs.getBoolean("isopenfin"));
		h.setOpenFout(rs.getBoolean("isopenfout"));
		h.setOpenPin(rs.getBoolean("isopenpin"));
		h.setOpenFrel(rs.getBoolean("isopenfrel"));
		h.setOpenAlert(rs.getBoolean("isopenalert"));
		
		return h;
	}
	
	public void run() {
		try {
			BigDecimal min, max;
			String subTopic = topic.substring(0, topic.indexOf("i3") + 3) + "sub";
			
			Class.forName(MYSQL_DRIVER);
			Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS);
			
			String sql = "select t.id as topic_id, t.name, m.id as message_id, m.message, m.rgb, h.id as house_id, r.* from topic t " +
						"left outer join nodemcu n on t.nodemcu_id = n.id " +
						"left outer join house h on h.nodemcu_id = n.id " +
						"left outer join mushroom r on r.id = h.mushroom_id " +
						"left outer join message m on m.topic_id = t.id " +
						"where t.name = '" + topic + "'";
			PreparedStatement stm = conn.prepareStatement(sql);
			ResultSet rs = stm.executeQuery();
			rs.first();
			long topicId = rs.getLong("topic_id");
			long houseId = rs.getLong("house_id");
			if(houseId != 0) {
				House h = getHouseById(conn, houseId);
				
				if(topic.indexOf("/temp/in") != -1) {
					min = rs.getBigDecimal("temperaturemin");
					max = rs.getBigDecimal("temperaturemax");
					if((new BigDecimal(new String(message.getPayload()))).compareTo(min) < 0) {
						int compare = ifInTempGtOutTemp(conn, new BigDecimal(new String(message.getPayload())));
						if(compare == 1) {
							if(h.isOpenFout()) {
								sendControl(CLOSE_FOUT);
								h.setOpenFout(CLOSE);
								//System.out.println("Alert เตือน อุณหภูมิ ภายใน ต่ำเกิน แต่ สูงกว่าภายนอก");
							}
							if(h.isOpenFin()) {
								sendControl(CLOSE_FIN);
								h.setOpenFin(CLOSE);
							}
							if(!h.isOpenAlert()) {
								//sendControl(ALERT);
								(new Thread(new Alert(myClient, subTopic))).start();
								h.setOpenAlert(OPEN);
							}
						} else if(compare == -1) {
							if(!h.isOpenFin()) {
								sendControl(OPEN_FIN);
								h.setOpenFin(OPEN);
								//System.out.println("อุณหภูมิ ภายใน ต่ำเกิน แต่ ต่ำกว่าภายนอก");
							}
						}
					} else if((new BigDecimal(new String(message.getPayload()))).compareTo(max) > 0) {
						if(!h.isOpenFout()) {
							sendControl(OPEN_FOUT);
							h.setOpenFout(OPEN);
							//System.out.println("อุณหภูมิ ภายใน ร้อนเกิน");
						}
					} else {
						if(h.isOpenFout()) {
							sendControl(CLOSE_FOUT);
							h.setOpenFout(CLOSE);
							//System.out.println("อุณหภูมิ ภายใน ปรกติ");
						}
						if(h.isOpenFin()) {
							sendControl(CLOSE_FIN);
							h.setOpenFin(CLOSE);
						}
						h.setOpenAlert(CLOSE);
						
					}
				} else if(topic.indexOf("/temp/out") != -1) {
					min = rs.getBigDecimal("temperatureoutmin");
					max = rs.getBigDecimal("temperatureoutmax");
					if((new BigDecimal(new String(message.getPayload()))).compareTo(min) < 0) {
						if(h.isOpenPout()) {
							sendControl(CLOSE_POUT);
							h.setOpenPout(CLOSE);
							//System.out.println("อุณหภูมิ ภายนอก ปรกติ < 0");
						}
						
					} else if((new BigDecimal(new String(message.getPayload()))).compareTo(max) > 0) {
						if(!h.isOpenPout()) {
							sendControl(OPEN_POUT);
							h.setOpenPout(OPEN);
							//System.out.println("อุณหภูมิ ภายนอก ร้อนเกิน");
						}
					} else {
						if(h.isOpenPout()) {
							sendControl(CLOSE_POUT);
							h.setOpenPout(CLOSE);
							//System.out.println("อุณหภูมิ ภายนอก ปรกติ");
						};
					}
				} else if(topic.indexOf("/hum/in") != -1) {
					min = rs.getBigDecimal("humiditymin");
					max = rs.getBigDecimal("humiditymax");
					if((new BigDecimal(new String(message.getPayload()))).compareTo(min) < 0) {
						if(!h.isOpenPin()) {
							sendControl(OPEN_PIN);
							h.setOpenPin(OPEN);
							//System.out.println("ความชื้น ต่ำ เพราะ อุณหภูมิ ภายใน ร้อนเกิน");
						}
						if(!h.isOpenFrel()) {
							sendControl(OPEN_FREL);
							h.setOpenFrel(OPEN);
						}
					} else if((new BigDecimal(new String(message.getPayload()))).compareTo(max) > 0) {
						if(h.isOpenPin()) {
							sendControl(CLOSE_PIN);
							h.setOpenPin(CLOSE);
							//System.out.println("Alert ความชื้น สูง เพราะ อุณหภูมิ ภายใน ต่ำเกิน แต่ สูงกว่าภายนอก");
						}
						if(!h.isOpenFrel()) {
							sendControl(OPEN_FREL);
							h.setOpenFrel(OPEN);
						}
						if(!h.isOpenAlert()) {
							//sendControl(ALERT);
							(new Thread(new Alert(myClient, subTopic))).start();
							h.setOpenAlert(OPEN);
						}
					} else {
						if(h.isOpenPin()) {
							sendControl(CLOSE_PIN);
							h.setOpenPin(CLOSE);
							//System.out.println("ความชื้น ปรกติ");
						}
						if(h.isOpenFrel()) {
							sendControl(CLOSE_FREL);
							h.setOpenFrel(CLOSE);
						}
						h.setOpenAlert(CLOSE);
					}
				}
				sql = "update house set isopenpout = "+h.isOpenPout()+", isopenfin = " +h.isOpenFin() + ", isopenalert = " + h.isOpenAlert() +
						", isopenfout = "+h.isOpenFout()+", isopenpin = "+h.isOpenPin()+", isopenfrel = "+h.isOpenFrel()+", update_date = now() where id = " + h.getId();
				
				stm = conn.prepareStatement(sql);
				stm.executeUpdate();
			}
			
			if(rs.getBigDecimal("message") != null) {
				sql = "update message set message = " + new BigDecimal(new String(message.getPayload())) + ", update_date = now() where topic_id = " + topicId;
			} else {
				sql = "insert into message (message, update_date, topic_id) values(" + new BigDecimal(new String(message.getPayload())) + ", now(), " + topicId + ")";
			}
			
			stm = conn.prepareStatement(sql);
			stm.executeUpdate();			
			
			sql = "insert into messagehistory (message, update_date, topic_id) values(" + new BigDecimal(new String(message.getPayload())) + ", now(), " + topicId + ")";
			stm = conn.prepareStatement(sql);
			stm.executeUpdate();
			
			//System.out.println(sql);
			
			rs.close();
			stm.close();
			conn.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}