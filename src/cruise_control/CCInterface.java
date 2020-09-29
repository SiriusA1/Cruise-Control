package cruise_control;

public class CCInterface {
	/* object to represent the steering wheel interface where the user
	 * can set their preferred speed
	 */
	private int minSpeed = 15;
	private int maxSpeed = 100;
	private int prefSpeed;
	private boolean IO;
	private boolean enabled;
	
	protected CCInterface() {
		this.IO = false;
		this.enabled = false;
		this.prefSpeed = -1;
	}
	protected int getSpeed() {
		return this.prefSpeed;
	}
	protected boolean getCruiseIO() {
		return this.IO;
	}
	protected boolean getCruiseState() {
		return this.enabled;
	}
	protected void setMinSpeed(int speed) {
		this.minSpeed = speed;
	}
	protected void setMaxSpeed(int speed) {
		this.maxSpeed = speed;
	}
	//only used for brake/clutch
	protected void setDisabled() {
		this.enabled = false;
	}
	protected void powerButton() {
		this.IO = !this.IO;
	}
	//If cruise is disabled, resumes from set cruise speed (assuming one exists)
	//If cruise is enabled, increases cruise speed in increments of 1
	protected boolean plusButton(EMS ems) {
		//resume function
		if(this.IO && !this.enabled && this.prefSpeed >= minSpeed && this.prefSpeed <= maxSpeed) {
			ems.sendSpeed(this.prefSpeed);
			this.enabled = true;
			return true;
		}
		//plus function
		if(this.IO && this.prefSpeed > 0 && this.prefSpeed + 1 < maxSpeed) {
			this.prefSpeed += 1;
			ems.sendSpeed(this.prefSpeed);
			ems.incSpeed();
			return true;
		}
		return false;
	}
	//If cruise is disabled, sets cruise speed at current speed
	//If cruise is enabled, decreases cruise speed in increments of 1
	protected boolean minusButton(EMS ems) {
		//set function
		if(this.IO && !this.enabled) {
			if(ems.getCurrentSpeed() > minSpeed && ems.getCurrentSpeed() < maxSpeed) {
				this.prefSpeed = ems.getCurrentSpeed();
				this.enabled = true;
				ems.sendSpeed(this.prefSpeed);
				return true;
			}
		}
		//minus function
		if(this.IO && this.prefSpeed - 1 > minSpeed) {
			this.prefSpeed -= 1;
			ems.sendSpeed(this.prefSpeed);
			ems.incSpeed();
			return true;
		}
		return false;
	}
}
