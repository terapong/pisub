package pisub;

import java.io.Serializable;
import java.util.Date;

public class House implements Serializable{
	private static final long serialVersionUID = 1L;
	private long id;
	private String name;
	
	private boolean isOpenPout = false;
	private boolean isOpenFin = false;
	private boolean isOpenFout = false;
	private boolean isOpenPin = false;
	private boolean isOpenFrel = false;
	private boolean isOpenAlert = false;
	
	private Date updateDate;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public boolean isOpenPout() {
		return isOpenPout;
	}

	public void setOpenPout(boolean isOpenPout) {
		this.isOpenPout = isOpenPout;
	}

	public boolean isOpenFin() {
		return isOpenFin;
	}

	public void setOpenFin(boolean isOpenFin) {
		this.isOpenFin = isOpenFin;
	}

	public boolean isOpenFout() {
		return isOpenFout;
	}

	public void setOpenFout(boolean isOpenFout) {
		this.isOpenFout = isOpenFout;
	}

	public boolean isOpenPin() {
		return isOpenPin;
	}

	public void setOpenPin(boolean isOpenPin) {
		this.isOpenPin = isOpenPin;
	}

	public boolean isOpenFrel() {
		return isOpenFrel;
	}

	public void setOpenFrel(boolean isOpenFrel) {
		this.isOpenFrel = isOpenFrel;
	}

	public boolean isOpenAlert() {
		return isOpenAlert;
	}

	public void setOpenAlert(boolean isOpenAlert) {
		this.isOpenAlert = isOpenAlert;
	}
}
