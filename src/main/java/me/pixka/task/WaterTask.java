package me.pixka.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import me.pixka.device.task.LoadDeviceConfigTask;
import me.pixka.device.task.LoadWatertimingConfigTask;
import me.pixka.service.WaterService_backup;
import me.pixka.service.WaterServiceii;

@Component
public class WaterTask implements CommandLineRunner {

	@Autowired
	private WaterServiceii waterservice;
	@Autowired
	private LoadWatertimingConfigTask wtask;
	@Autowired
	private LoadDeviceConfigTask dtask;

	@Override
	public void run(String... arg0) throws Exception {
		System.out.println(
				"--------------------------------------- Water Start --------------------------------------------------");
		// Thread.sleep(10*1000);
		// waterService.loadDeviceConfig();
		// waterService.loadWaterConfig();
		// waterService.readDs();

		waterservice.run();
		wtask.run();
		dtask.run();

	}

}
