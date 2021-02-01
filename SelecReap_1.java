import java.awt.Color;
public class SelecReap_1 {
	protected static int window_len, pack_width, pack_height, h_offset, v_offset, v_clearance, total_Packet, time_out_sec;
	protected static final Color unack_color = new Color(145, 189, 219);
	protected  static final Color received_ack = new Color(2, 171, 165);
	protected static final Color roam_pack_color = new Color(199, 178, 93);
	protected static final Color ack_color = Color.yellow;
	protected static final Color sel_color = new Color(40, 122, 5);
	protected static final Color roam_ack_color = new Color(209, 63, 190);
	protected static final Color dest_color = Color.red;
	protected static final int ADVANCE_PACKET = 5;
	protected static final int sender_window_len_def = 5;
	protected static final int receiver_window_len = 5;
	protected static final int TIMEOUT_MULTIPLIER = 1000;
	protected static final int MIN_FPS = 3;
	protected static final int FPS_STEP = 2;
	protected static final int DEFAULT_FPS = 5;
	protected static final int total_Packet_def = 20;
	protected static final int time_out_sec_def = 25;
	protected static final int pack_width_def = 30;
	protected static final int pack_height_def = 40;
	protected  static final int h_offset_def = 100;
	protected  static final int v_offset_def = 50;
	protected  static final int v_clearance_def = 300;
    

}
