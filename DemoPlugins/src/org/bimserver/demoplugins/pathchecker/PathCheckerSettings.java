package org.bimserver.demoplugins.pathchecker;

public class PathCheckerSettings {
	private float minHeadRoomMM = 2000;
	private float minWidth = 1500;
	
	public float getMinHeadRoomMM() {
		return minHeadRoomMM;
	}
	public void setMinHeadRoomMM(float minHeadRoomMM) {
		this.minHeadRoomMM = minHeadRoomMM;
	}
	public float getMinWidth() {
		return minWidth;
	}
	public void setMinWidth(float minWidth) {
		this.minWidth = minWidth;
	}
	
	
}
