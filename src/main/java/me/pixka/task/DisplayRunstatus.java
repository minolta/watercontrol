package me.pixka.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import me.pixka.device.s.DisplayService;

@Component
public class DisplayRunstatus implements CommandLineRunner {

	@Autowired
	private DisplayService displayService;
	@Override
	public void run(String... arg0) throws Exception {

		
		displayService.printRunstatus(); //รวมกันไว้แล้ว
		displayService.readDs();
	//	displayService.printDsvalue();
		displayService.checkLCDon();
	}

}
