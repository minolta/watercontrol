package me.pixka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import me.pixka.c.Font;
import me.pixka.c.Led;

@Component
public class Runstatus {
	@Autowired
	private Led led;

	@Scheduled(fixedDelay = 2000)
	public void printRunstatus() {
		System.out.println("print status ");
		led.letter((short) 3, (short) '.', Font.CP437_FONT, true);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		led.letter((short) 3, (short) ' ', Font.CP437_FONT, true);

	}

}
