package me.pixka.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import me.pixka.device.s.Ds18b20Service;

@Component
public class Ds18b20Task implements CommandLineRunner {
	@Autowired
	private Ds18b20Service ds18b20Service;

	@Override
	public void run(String... arg0) throws Exception {

		System.out.println("Start ----------------------------------------> Ds18b20 ");
		ds18b20Service.send();
	}

}
