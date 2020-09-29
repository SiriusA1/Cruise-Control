package cruise_control;
import java.io.IOException;
import java.lang.String;
import java.lang.System;
import java.util.logging.Logger;
import java.util.Scanner;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;
import java.util.logging.FileHandler;


public class BridgeData {
	/* USER MANUAL:
	 * press gas %d: presses gas pedal and changes speed to %d
	 * press brake: presses brake
	 * press clutch: presses clutch
	 * power: powers on/off cruise control
	 * plus/resume button: resumes from previous cruise speed if cruise is disabled, increases cruise speed by 1 when cruise is enabled
	 * minus/set button: sets cruise speed at current speed if cruise is disabled, decreases cruise speed by 1 when cruise is enabled
	 * turn off engine: shuts off car and completely powers off cruise control program
	 * release gas: releases gas pedal, only has a function when cruise is enabled
	 * release clutch: releases clutch pedal
	 * release brake: releases brake pedal
	 * login: attempt to login to admin account
	 */
	private static Logger logger = Logger.getLogger(BridgeData.class.getName());
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
	//"Time: " + dtf.format(LocalDateTime.now()) + "   -EVENT-" //example of how logging format

	//need custom thread to send speed to EMS while main method is running
	class SendsSpeedThread extends Thread {
		private volatile boolean running;
		private volatile boolean sending;

		public SendsSpeedThread() {
			this.running = true;
			this.sending = false;
		}

		public void run() {
			while(this.running) {
				if(this.sending) {
					long prevTime = System.nanoTime();
					long numSends = 0;
					logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -SENDING DESIRED SPEED TO EMS-");
					while(this.sending) {	
						if((System.nanoTime() - prevTime) / 1000000 > 100) {
							++numSends;
							prevTime = System.nanoTime();
						}
					}
					logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -SENT DESIRED SPEED TO EMS " + numSends + " TIMES-");
				}
			}
		}

		public void send() {
			this.sending = true;
		}
		public void stopSending() {
			this.sending = false;
		}
		public void stopRunning() {
			this.running = false;
		}
	}

	public static void main(String[] args) { 
		/* STILL NEED:
		 * Change log so only admin can access it
		 * Change min and max speed cruise properties so the admin can change them
		 */

		Scanner in = new Scanner(System.in);

		//all logs to the logger should start with the time
		//CC turns on when engine turns on

		Brake brake = new Brake();
		Gas gas = new Gas();
		Admin admin = new Admin();
		Clutch clutch = new Clutch();
		CCInterface ccInterface = new CCInterface();
		EMS ems = new EMS();
		Engine engine = new Engine();
		SendsSpeedThread thread = new BridgeData().new SendsSpeedThread();
		String currentDir = System.getProperty("user.dir");
		try {
			FileHandler fh = new FileHandler(currentDir + "\\log.log");
			logger.addHandler(fh);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CAR ENGINE IS RUNNING-");

		thread.start();

		//while engine is on
		engine_on :
			while(engine.getState()) {
				String input = in.nextLine();

				if(input.equalsIgnoreCase("power")) {
					ccInterface.powerButton();
					logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE CONTROL POWERED ON-");
				}

				if(input.length() > 9 && input.substring(0,9).equalsIgnoreCase("press gas")) {
					if(!gas.getState()) {
						logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -GAS PRESSED-");
						gas.updateState();
					}
					int temp = Integer.parseInt(input.substring(10));
					ems.setCurrentSpeed(temp);
					logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -DRIVING AT " + temp + "MPH-");
				}

				//user turns off engine
				if(input.equalsIgnoreCase("turn off engine")) {
					logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -ENGINE TURNED OFF-");
					engine.updateState();
					thread.stopSending();
					thread.stopRunning();
					break engine_on;
				}

				//user tries to release brake, only works if brake was already pressed
				if(input.equalsIgnoreCase("release brake")) {
					if(brake.getState()) {
						logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -BRAKE RELEASED-");
						brake.updateState();
					}else {
						logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -BRAKE COULD NOT BE RELEASED-");
					}
				}

				//user tries to release clutch, only works if clutch was already pressed
				if(input.equalsIgnoreCase("release clutch")) {
					if(clutch.getState()) {
						logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CLUTCH RELEASED-");
						clutch.updateState();
					}else {
						logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CLUTCH COULD NOT BE RELEASED-");
					}
				}

				//while cruise control is powered on
				cc_on:
					while(ccInterface.getCruiseIO()) {
						input = in.nextLine();
						//user drives at set speed, type 'press gas x' with x being speed in mph
						if(input.length() > 9 && input.substring(0,9).equalsIgnoreCase("press gas")) {
							if(!gas.getState()) {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -GAS PRESSED-");
								gas.updateState();
							}
							int temp = Integer.parseInt(input.substring(10));
							ems.setCurrentSpeed(temp);
							logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -DRIVING AT " + temp + "MPH-");
						}

						//user turns off engine
						if(input.equalsIgnoreCase("turn off engine")) {
							logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -ENGINE TURNED OFF, CRUISE CONTROL POWERED OFF-");
							engine.updateState();
							thread.stopSending();
							thread.stopRunning();
							break engine_on;
						}

						//admin logs in while car isn't moving
						if(input.equalsIgnoreCase("login") && ems.getCurrentSpeed() == 0) {
							logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -ATTEMPTING ADMIN LOGIN-");
							System.out.println("Username: ");
							String user = in.nextLine();
							System.out.println("Password: ");
							String pass = in.nextLine();
							if(admin.login(user, pass)) {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -LOGIN SUCCESSFUL-");
								boolean invalid = true;
								while(invalid) {
									System.out.println("Enter new max speed: ");
									int max = in.nextInt();
									System.out.println("Enter new min speed: ");
									int min = in.nextInt();
									if(max < 0) {
										System.out.println("Max speed cannot be less than 0.");
									}else if(min < 0) {
										System.out.println("Min speed cannot be less than 0.");
									}else if(max <= min) {
										System.out.println("Max speed cannot be less than or equal to min speed.");
									}else {
										ccInterface.setMaxSpeed(max);
										ccInterface.setMinSpeed(min);
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -NEW MAX SPEED SET AT " + max + " AND NEW MIN SPEED SET AT " + min + "-");
										invalid = false;
									}
								}
							}else {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -LOGIN DENIED-");
							}
						}

						//user presses cruise button
						if(input.equalsIgnoreCase("power")) {
							ccInterface.powerButton();
							logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE CONTROL POWERED OFF-");
							break cc_on;
						}

						//user tries to release brake, only works if brake was already pressed
						if(input.equalsIgnoreCase("release brake")) {
							if(brake.getState()) {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -BRAKE RELEASED-");
								brake.updateState();
							}else {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -BRAKE COULD NOT BE RELEASED-");
							}
						}

						//user tries to release clutch, only works if clutch was already pressed
						if(input.equalsIgnoreCase("release clutch")) {
							if(clutch.getState()) {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CLUTCH RELEASED-");
								clutch.updateState();
							}else {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CLUTCH COULD NOT BE RELEASED-");
							}
						}

						//user presses plus/resume button
						if(input.equalsIgnoreCase("plus/resume button")) {
							logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -RESUME BUTTON PRESSED-");
							if(!brake.getState() && ccInterface.plusButton(ems)) {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE RESUME AT " + ems.getCCSpeed() + "MPH-");
								int temp = ems.getCurrentSpeed();
								if(temp > ems.getCCSpeed()) {
									logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -DECELERATING FROM " + temp + "-");
									ems.setCurrentSpeed(ccInterface.getSpeed());
								}else if(temp < ems.getCCSpeed()) {
									logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -ACCELERATING FROM " + temp + "-");
									ems.setCurrentSpeed(ccInterface.getSpeed());
								}
								thread.send();
							}else {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE COULD NOT BE RESUMED-");
							}
						}

						//user presses minus/set button
						if(input.equalsIgnoreCase("minus/set button")) {
							logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -SET BUTTON PRESSED-");
							if(!brake.getState() && ccInterface.minusButton(ems)) {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE SPEED SET TO " + ems.getCCSpeed() + "MPH-");
								thread.send();
							}else {
								logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE SPEED COULD NOT BE SET-");
							}
						}

						//while cruise is enabled
						c_on :
							while(ccInterface.getCruiseState() && !brake.getState()) {

								ems.sendSpeed(ccInterface.getSpeed());
								input = in.nextLine();

								//user turns off engine
								if(input.equalsIgnoreCase("turn off engine")) {
									logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE CONTROL POWERED OFF-");
									engine.updateState();
									thread.stopSending();
									thread.stopRunning();
									break engine_on;
								}

								//user tries to release gas, only works if gas was already pressed
								if(input.equalsIgnoreCase("release gas")) {
									if(gas.getState()) {
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -GAS RELEASED-");
										brake.updateState();
									}else {
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -GAS COULD NOT BE RELEASED-");
									}
								}

								//user presses brake
								if(input.equalsIgnoreCase("press brake")) {
									logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -BRAKE PRESSED-");
									brake.updateState();
									//pause CC
									ccInterface.setDisabled();
									logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE DISABLED-");
									thread.stopSending();
									break c_on;
								}

								//user presses power button
								if(input.equalsIgnoreCase("power")) {
									ccInterface.powerButton();
									logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE CONTROL POWERED OFF-");
									thread.stopSending();
									break cc_on;
								}

								//user presses plus/resume button
								if(input.equalsIgnoreCase("plus/resume button")) {
									int temp = ems.getCurrentSpeed();
									logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -PLUS BUTTON PRESSED-");
									if(ccInterface.plusButton(ems)) {
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE SPEED INCREASED TO " + ems.getCCSpeed() + "-");
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -ACCELERATING FROM " + temp + "-");
									}else {
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE SPEED COULD NOT BE INCREASED-");
									}
								}

								//user presses minus/set button
								if(input.equalsIgnoreCase("minus/set button")) {
									int temp = ems.getCurrentSpeed();
									logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -MINUS BUTTON PRESSED-");
									if(ccInterface.minusButton(ems)) {
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE SPEED DECREASED TO " + ems.getCCSpeed() + "-");
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -DECELERATING FROM " + temp + "-");
									}else {
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CRUISE SPEED COULD NOT BE DECREASED-");
									}
								}

								//user changes speed while cruise is enabled, type 'press gas x' with x being speed in mph
								if(input.length() > 9 && input.substring(0,9).equalsIgnoreCase("press gas")) {
									if(!gas.getState()) {
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -GAS PRESSED-");
										gas.updateState();
									}
									int temp = Integer.parseInt(input.substring(10));
									if(temp > ems.getCCSpeed()) {
										ems.setCurrentSpeed(temp);
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -USER IS ACCELERATING-");
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -DRIVING AT " + temp + "MPH-");
										logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -PAUSING CRUISE-");
										ccInterface.setDisabled();
										thread.stopSending();
										break c_on;
									}
								}

								//user presses clutch, pauses CC
								if(input.equalsIgnoreCase("press clutch")) {
									logger.info("Time: " + dtf.format(LocalDateTime.now()) + "  -CLUTCH PRESSED-");
									clutch.updateState();
									thread.stopSending();
									break c_on;
								}
							}
					}
			}
		in.close();
	}
}
