package cruise_control;

public class Clutch {
	
	//basically whether the sensor is activated or not.
	//false = unpressed, true = pressed
	private boolean clutchState;
	
	protected Clutch() {
		this.clutchState = false;
	}
	protected boolean getState() {
		return clutchState;
	}
	protected void updateState() {
		this.clutchState = !this.clutchState;
	}

}