package pisub;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class Message implements Serializable{
	private static final long serialVersionUID = 1L;
	private long id;
	private BigDecimal message;
	private Date updateDate;
	private String rgb;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public BigDecimal getMessage() {
		return message;
	}

	public void setMessage(BigDecimal message) {
		this.message = message;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public String getRgb() {
		return rgb;
	}

	public void setRgb(String rgb) {
		this.rgb = rgb;
	}
}
