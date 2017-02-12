package me.pixka;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import me.pixka.c.Ds18b20;
import me.pixka.c.HttpControl;
import me.pixka.c.PiDevice;

@Component
public class Senddata {

	@Autowired
	private PiDevice pi;
	@Autowired
	private Ds18b20 ds;
	@Autowired
	private HttpControl http;

	@Scheduled(fixedRate = 60 * 1000)
	public void send() {
		BigDecimal data = null;
		try {
			data = ds.read();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		send("61.19.255.23", "3333", data, pi.wifi());
	}

	public void send(String host, String port, BigDecimal tmp, String mac) {
		String s = "http://" + host + ":" + port + "/addds?t=" + tmp + "&m=" + mac;
		try {
			http.get(s);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
