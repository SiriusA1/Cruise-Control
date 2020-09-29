package cruise_control;

public class Admin {

	private final String ADMIN_USER = "Admin";
	private String ADMIN_PASS = "CoolControl";
	
	protected Admin() {
	}
	
	protected boolean login(String user, String pass) {
		if(this.ADMIN_USER.equals(user) && this.ADMIN_PASS.equals(pass)) {
			return true;
		}
		return false;
	}
}
