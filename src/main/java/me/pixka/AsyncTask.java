package me.pixka;

import org.springframework.scheduling.annotation.Async;

public class AsyncTask {
	@Async
	public void doAsyncTask() {
		System.out.println("do some async task");
	}
}
