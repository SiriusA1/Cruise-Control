package cruise_control;

public class Gas {
	
	//basically whether the sensor is activated or not.
	//false = unpressed, true = pressed
	private boolean gasState;
	
	protected Gas() {
		this.gasState = false;
	}
	protected boolean getState() {
		return gasState;
	}
	protected void updateState() {
		this.gasState = !this.gasState;
	}

}
