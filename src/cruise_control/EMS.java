package cruise_control;

public class EMS {
	private int ccSpeed = -1;
	//not sure what to initialize this at, this is just an example
	private int currSpeed;
	protected EMS() {
		this.ccSpeed = -1;
		this.currSpeed = 0;
	}
	protected void sendSpeed(int speed) {
		//sends speed to EMS, since there's no actual hardware this won't really do anything
		this.ccSpeed = speed;
	}
	protected void incSpeed() {
		this.currSpeed = this.ccSpeed;
	}
	protected void setCurrentSpeed(int speed) {
		this.currSpeed = speed;
	}
	protected int getCCSpeed() {
		return this.ccSpeed;
	}
	protected int getCurrentSpeed() {
		return this.currSpeed;
	}
}
