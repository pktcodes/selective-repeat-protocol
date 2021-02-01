
class SelectiveRepeatPacket {
	boolean on_way; 
	boolean reached_dest; 
	boolean acknowledged; 
	boolean Packet_ack; 
	boolean selected; 
	boolean received; 
	boolean out_of_order; 
	int Packet_pos; 
	int ackFor; 
	boolean buffered;
	SelectiveRepeat.PacketTimerTask packet_timer_task;

	SelectiveRepeatPacket() 
	{
		on_way = false;
		selected = false;
		reached_dest = false;
		acknowledged = false;
		Packet_ack = true;
		received = false;
		out_of_order = false;
		Packet_pos = 0;
		ackFor = 0;
		buffered = false;
	}

	SelectiveRepeatPacket(boolean onway, int Packetpos, int nextseq) 
	{
		on_way = onway;
		selected = false;
		reached_dest = false;
		acknowledged = false;
		Packet_ack = true;
		received = false;
		out_of_order = false;
		Packet_pos = Packetpos;
		ackFor = nextseq;
		buffered = false;
	}
}