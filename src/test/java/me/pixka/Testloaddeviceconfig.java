package me.pixka;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import me.pixka.device.d.Deviceconfig;
import me.pixka.device.d.Watertiming;
import me.pixka.device.s.DeviceconfigService;
import me.pixka.device.s.WatertimingService;
import me.pixka.device.task.LoadWatertimingConfigTask;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Testloaddeviceconfig {
	
	@Autowired
	private LoadWatertimingConfigTask task;
	@Autowired 
	private WatertimingService service;
	@Test
	public void test() throws InterruptedException
	{	
		task.setMac("98ded014a86e");
		List list = task.load(0L);
		System.out.println("Found  ====================== >>>"+list.size());
		
		List<Deviceconfig> ds = list;
		System.out.println("List:"+list);
		
		Iterator items = list.iterator();
		
		while(items.hasNext())
		{
			//System.out.println("Save: "+items.next());
			
			Watertiming i =  (Watertiming) items.next();
			i.setDevice(null);
			i.setDevice_id(null);
			service.save(i);
			System.out.println(i);
		}
		Thread.sleep(10000);
	}

}
