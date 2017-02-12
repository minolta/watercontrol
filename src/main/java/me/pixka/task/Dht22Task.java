package me.pixka.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import me.pixka.device.s.Dht22Service;

@Component
public class Dht22Task implements CommandLineRunner {

	@Autowired
	private Dht22Service dht22Service;

	@Override
	public void run(String... arg0) throws Exception {
		dht22Service.read();
	}

}
