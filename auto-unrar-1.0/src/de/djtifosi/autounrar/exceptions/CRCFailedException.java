package de.djtifosi.autounrar.exceptions;

public class CRCFailedException extends Exception {
	
	private static final long serialVersionUID = -2836120418533018324L;

	private String errorStream;

	public String getErrorStream() {
		return errorStream;
	}

	public CRCFailedException(String errorStream) {
		this.errorStream = errorStream;
	}

}
