import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JOptionPane;

public class SelectiveRepeat extends Applet implements Runnable,ActionListener{   

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	int lastKnownSucPacket,window_len, pack_width, pack_height, base, receiver_base, nextseqsum, fps, selected = -1, timeout,
			timeoutPacket,  h_offset, v_offset, v_clearance, total_Packet, time_out_sec;

	boolean timerFlag, timerSleep;


	Button send, stop, fast, slow, kill, reset;
	Thread gbnThread;  


	PausableThreadPoolExecutor pausableThreadPoolExecutor;

	SelectiveRepeatPacket SenderPack[];
	TextArea output; 
	Dimension offDimension;
	Image offImage,statsImage; 
	Graphics offGraphics,SG;


	private void initalizeDesign() {

		send = new Button("SEND");
		send.setActionCommand("newpacket");
		send.addActionListener(this);
		send.setBounds(20, 2, 90, 30);


		stop = new Button("PAUSE");
		stop.setActionCommand("pause");
		stop.addActionListener(this);
		stop.setBounds(110, 2, 90, 30);

		fast = new Button("FAST");
		fast.setActionCommand("faster");
		fast.addActionListener(this);
		fast.setBounds(200, 2, 90, 30);

		slow = new Button("SLOW");
		slow.setActionCommand("slow");
		slow.addActionListener(this);
		slow.setBounds(290, 2, 90, 30);

		kill = new Button("KILL");
		kill.setActionCommand("kill");
		kill.addActionListener(this);
		kill.setEnabled(false);
		kill.setBounds(380, 2, 90, 30);

		reset = new Button("RESET");
		reset.setActionCommand("restart");
		reset.addActionListener(this);
		reset.setBounds(470, 2, 90, 30);

		// Adding the buttons to our applet window so they can be rendered and used
		add(send);
		add(stop);
		add(fast);
		add(slow);
		add(kill);
		add(reset);
		output.append("*****Started*****\n");
	}

	public void init(){
		setLayout(null);  
		output = new TextArea(250, 250); 
		output.setBounds(100, 400, 680, 220); 
		output.setEditable(false); 
		add(output); 
		SSP();
		initalize(); 
		initalizeDesign();
	}

	public void run(){
		System.gc(); 
		boolean stopCheck = false;
		if (SenderPack[total_Packet - 1] != null){
			for (int i = total_Packet - window_len; i < total_Packet; i++){
				if (SenderPack[i].acknowledged==false){
					stopCheck = false;
					break;
				}else{
					stopCheck = true;
				}
			}
			if (stopCheck){
				output.append("Data Transferred - Simulation completed.\n");
				gbnThread = null;
				return;
			}
		}
		Thread currenthread = Thread.currentThread();
		while (currenthread == gbnThread){

			if (onTheWay(SenderPack)) { 
				for (int i = 0; i < total_Packet; i++){
					if (SenderPack[i] != null){	
						if(SenderPack[i].packet_timer_task != null){
							SenderPack[i].packet_timer_task.current_index = i;
						}

						if (SenderPack[i].on_way) {
							if (SenderPack[i].Packet_pos < (v_clearance - pack_height)){
								SenderPack[i].Packet_pos += 5;
							}else if (SenderPack[i].Packet_ack){
								SenderPack[i].reached_dest = true;
								if (check_upto_n(i)) {
									SenderPack[i].Packet_pos = pack_height + 5;
									SenderPack[i].Packet_ack = false;

									if (SenderPack[i].buffered || SenderPack[i].acknowledged) 
									{
										output.append("(R) - Packet " + i + " received. Selective acknowledge for only Packet " + i + " sent.\n");
										SenderPack[i].received = true;
									} 
									else if (!SenderPack[i].received) 
									{
										output.append("(R) - Packet " + i + " received. Selective acknowledge for only Packet " + i + " sent. Packet " + i + " delivered to application.\n");
										SenderPack[i].received = true;
									} 
									else
									{
										output.append("(R) - Packet " + i + " received out of order. Selective acknowledge for only Packet " + i + " sent again(DUPLICATE PACK)\n");
									}
									SenderPack[i].received = true;
									deliverBuffer(i);
								}

								else if (SenderPack[i].acknowledged) 
								{
									SenderPack[i].Packet_pos = pack_height + 5;
									SenderPack[i].Packet_ack = false;
									output.append("(R) - Packet " + i + " received. Selective acknowledge for only Packet " + i + " sent.\n");
									SenderPack[i].received = true;
									deliverBuffer(i);
								}else {
									SenderPack[i].buffered = true;
									SenderPack[i].Packet_pos = pack_height + 5;
									SenderPack[i].Packet_ack = false;
									output.append("(R) - Packet " + i + " received out of order.  Packet buffered. Selective acknowledge for only Packet " + i + " sent.\n");
									SenderPack[i].received = true;
									deliverBuffer(i);
									if (i == selected){
										selected = -1;
										kill.setEnabled(false);
									}
								}
							} else if (!SenderPack[i].Packet_ack){
								output.append("(S) - Selective ACK for only Packet " + i + " received. Timer for Packet " + i + " stopped.\n");
								SenderPack[i].on_way = false;


								if (check_upto_n(i)){
									SenderPack[i].acknowledged = true;
									SenderPack[i].buffered = false;
								}
								else 
								{
									SenderPack[i].acknowledged = true;
									SenderPack[i].buffered = true;
								}

								if (i == selected) 
								{
									selected = -1;
									kill.setEnabled(false);
								}


								if(SenderPack[i].packet_timer_task != null)	
									SenderPack[i].packet_timer_task.cancelTimer();


								for (int k = base; k < total_Packet; k++) 
								{
									if (SenderPack[k] != null) 
									{
										if (SenderPack[base].acknowledged) 
										{
											SenderPack[base].buffered = false;
											if (k + window_len < total_Packet)
											{
												base = base + 1;
											}
										}
									} 
									else
									{
										break;
									}
								}

								if (nextseqsum < base + window_len)
									send.setEnabled(true);

								if (base != nextseqsum) 
								{
									try 
									{
										if(SenderPack[i].packet_timer_task != null) 
										{	
											SenderPack[i].packet_timer_task.startTimer();
											pausableThreadPoolExecutor.schedule(SenderPack[i].packet_timer_task, 5, TimeUnit.SECONDS);
										}
									} 
									catch (IllegalStateException e)
									{

									}
								}
							}
						}
					}
				}
				repaint(); 

				try 
				{
					Thread.sleep(1000 / fps); 
				} 
				catch (InterruptedException e) 
				{
					System.out.println("Unable to run Timer Thread ");
				}
			} 
			else
			{
				gbnThread = null;
			}
		}
	}

	private void initalize() {
		base = 0; 
		receiver_base = 0; 
		nextseqsum = 0; 
		fps = 5;  
		SenderPack = new SelectiveRepeatPacket[total_Packet];
		pausableThreadPoolExecutor = new PausableThreadPoolExecutor(5);
	}
	public void startThread(){

		if (gbnThread == null)
			gbnThread = new Thread(this);
		gbnThread.start();
	}

	void deliverBuffer(int PacketNumber){
		int j = 0;
		while (j < PacketNumber){
			if (SenderPack[j] == null)
			{
				return;
			}else if (SenderPack[j].acknowledged){
				SenderPack[j].buffered = false;
				j++;
			} 
			else{
				break;
			}
		}

		if (j > 0)
			j--;
		for (int k = j; k < total_Packet; k++) 
		{

			if (SenderPack[k] == null)
				break;

			else if (SenderPack[k].buffered) 
			{
				SenderPack[k].buffered = false;

				output.append("(R) - Buffered Packet " + k + " delivered to application.\n");

			} 
			else if (SenderPack[k].acknowledged) 
			{
				SenderPack[k].acknowledged = true;
				SenderPack[k].buffered = false;
			} 
			else if (!SenderPack[k].Packet_ack) 
			{
				SenderPack[k].buffered = false;
			} 
			else
				break;
		}

		int count = 0;
		for (int i = 0; i < total_Packet; i++)
		{
			if (SenderPack[i] != null) 
			{
				if (SenderPack[i].received)
				{
					if (i + 1 <= (total_Packet - SelecReap_1.receiver_window_len))
						count = i + 1;
				}
				else 
				{
					break;
				}
			} 
			else
			{
				break;
			}
		}
		receiver_base = count;
	}


	private void RTOP(int index){
		int retransmitPacket = 0;
		if (SenderPack[index] != null)
		{
			if (!SenderPack[index].acknowledged && !SenderPack[index].buffered) 
			{
				SenderPack[index].on_way = true;
				SenderPack[index].Packet_ack = true;
				SenderPack[index].Packet_pos = pack_height + 5;
				retransmitPacket++;
			} 
			else if (!SenderPack[index].acknowledged && SenderPack[index].buffered) 
			{
				SenderPack[index].on_way = true;
				SenderPack[index].Packet_ack = true;
				SenderPack[index].Packet_pos = pack_height + 5;
				retransmitPacket++;
			}
		}

		if (gbnThread == null)
		{
			gbnThread = new Thread(this);
			gbnThread.start();
		}

		if (retransmitPacket == 0) 
		{
			SenderPack[index].packet_timer_task.cancelTimer();
		} 
		else 
		{
			output.append("(S) - Timeout occurred for Packet " + index + ". Timer restarted for Packet " + index + ". \n");
			SenderPack[index].packet_timer_task.startTimer();
			pausableThreadPoolExecutor.schedule(SenderPack[index].packet_timer_task, 5, TimeUnit.SECONDS);
		}
	}

	private void SSP(){
		try{ 
			window_len = SelecReap_1.sender_window_len_def;
			pack_width = SelecReap_1.pack_width_def;
			pack_height = SelecReap_1.pack_height_def;
			h_offset = SelecReap_1.h_offset_def;
			v_offset = SelecReap_1.v_offset_def;
			v_clearance = SelecReap_1.v_clearance_def;
			total_Packet = SelecReap_1.total_Packet_def;
			time_out_sec = (SelecReap_1.time_out_sec > 0) ? SelecReap_1.time_out_sec : SelecReap_1.time_out_sec_def;
		} 
		catch (Exception e){}
	}

	public void actionPerformed(ActionEvent e) {

		String cmd = e.getActionCommand();


		if (cmd == "newpacket" && nextseqsum < base + window_len){
			SenderPack[nextseqsum] = new SelectiveRepeatPacket(true, pack_height + SelecReap_1.ADVANCE_PACKET,nextseqsum);
			output.append("(S) - Packet " + nextseqsum + " sent.\n");
			output.append("(S) - Timer started for Packet " + nextseqsum + "\n");
			SenderPack[nextseqsum].packet_timer_task = new PacketTimerTask();
			SenderPack[nextseqsum].packet_timer_task.startTimer();
			pausableThreadPoolExecutor.schedule(SenderPack[nextseqsum].packet_timer_task, 5, TimeUnit.SECONDS);

			repaint();
			nextseqsum++;
			if (nextseqsum == base + window_len) //if maximum packets for the window is already set then disable the send button
				send.setEnabled(false);
			startThread();
		}
		else if (cmd == "faster"){
			fps += SelecReap_1.FPS_STEP;
			output.append("-Simulation speed increased\n");
		}
		else if (cmd == "slow" && fps > SelecReap_1.MIN_FPS) 
		{
			fps -= SelecReap_1.FPS_STEP;
			output.append("-Simulation speed decreased\n");
		}else if (cmd == "pause")
		{
			output.append("- Simulation paused\n");
			gbnThread = null;

			pausableThreadPoolExecutor.pause();


			stop.setLabel("Resume");
			stop.setActionCommand("startanim");
			send.setEnabled(false);
			slow.setEnabled(false);
			fast.setEnabled(false);
			kill.setEnabled(false);

			repaint();
		}

		else if (cmd == "startanim"){
			output.append("-Simulation resumed.\n");
			stop.setLabel("Pause");
			stop.setActionCommand("pause");

			pausableThreadPoolExecutor.resume();


			send.setEnabled(true);
			slow.setEnabled(true);
			fast.setEnabled(true);
			kill.setEnabled(false);


			repaint();
			startThread();
		}

		else if (cmd == "kill"){
			if(selected < 0)
			{
				JOptionPane.showMessageDialog(null,"Cannot Kill Packet As It is Not Selected ");
				kill.setEnabled(false);
				return;
			}
			if (SenderPack[selected].Packet_ack) 
			{
				output.append("- Packet " + selected + " lost\n");
			} 
			else
			{
				output.append("- Selective Ack of Packet " + selected + " lost.\n");
			}
			SenderPack[selected].on_way = false;
			kill.setEnabled(false);
			selected = -1;
			repaint();
		}else if (cmd == "restart")
		{
			reset_app();
		}
	}


	public boolean mouseDown(Event e, int x, int y){
		System.out.println("Mouse clicked At " + x + "," + y);
		int location, xpos, ypos;
		location = (x - h_offset) / (pack_width + 7);
		if (location >= total_Packet || location < 0){
			selected = -1;
			return false;
		}

		if (SenderPack[location] != null) {
			xpos = h_offset + (pack_width + 7) * location;
			ypos = SenderPack[location].Packet_pos;

			if (x >= xpos && x <= xpos + pack_width && SenderPack[location].on_way)
			{
				if ((SenderPack[location].Packet_ack && y >= v_offset + ypos &&
						y <= v_offset + ypos + pack_height) || 
						((!SenderPack[location].Packet_ack) && y >= v_offset + v_clearance - ypos && y <= v_offset + v_clearance - ypos + pack_height)) 
				{
					if (SenderPack[location].Packet_ack)
					{
						output.append("- Packet " + location + " selected Can be used for Killing .\n");
					}
					else
					{
						output.append("- Selective Ack " + location	+ " selected Can be used for Killing .\n");
					}

					SenderPack[location].selected = true;
					selected = location;
					kill.setEnabled(true);
					repaint();
				} 
				else 
				{
					output.append("-Click on a moving Packet to select.\n");
					selected = -1;
				}
			} 
			else 
			{
				output.append("-Click on a moving Packet to select.\n");
				selected = -1;
			}
		}

		return true;
	}


	public void paint(Graphics g){
		update(g);  
	}

	/* 
	 */

	public void update(Graphics g){
		@SuppressWarnings("deprecation")
		Dimension d = size();

		if ((offGraphics == null) || (d.width != offDimension.width)|| (d.height != offDimension.height)) {
			offDimension = d;
			offImage = createImage(d.width, d.height);
			offGraphics = offImage.getGraphics();
		}



		offGraphics.setColor(Color.white);
		offGraphics.fillRect(0, 0, d.width, d.height);

		if(SG == null){
			offDimension = d;
			statsImage = createImage(d.width, d.height);
			SG = statsImage.getGraphics();
			SG.setColor(Color.black);
			int newvOffset = v_offset + v_clearance + pack_height;

			int newHOffset = h_offset;
			SG.drawString("(S) - Action at Sender  (R) - Action at Receiver",newHOffset + 60, newvOffset + 90);
			SG.drawString("Packet", newHOffset + 15, newvOffset + 60);
			SG.drawString("Ack Received", newHOffset + 225,newvOffset + 60);
			SG.drawString("Ack", newHOffset + 170, newvOffset + 60);
			SG.drawString("Received", newHOffset + 85, newvOffset + 60);
			SG.drawString("Selected", newHOffset + 335, newvOffset + 60);
			SG.drawString("Buffered", newHOffset + 415, newvOffset + 60);

			SG.setColor(Color.gray);
			SG.draw3DRect(newHOffset - 10, newvOffset + 42, 475, 25, true);
			SG.setColor(SelecReap_1.roam_pack_color);
			SG.fill3DRect(newHOffset, newvOffset + 50, 10, 10, true);
			SG.setColor(SelecReap_1.roam_ack_color);
			SG.fill3DRect(newHOffset + 155, newvOffset + 50, 10, 10, true);
			SG.setColor(SelecReap_1.received_ack);
			SG.fill3DRect(newHOffset + 210, newvOffset + 50, 10, 10, true);
			SG.setColor(SelecReap_1.dest_color);
			SG.fill3DRect(newHOffset + 70, newvOffset + 50, 10, 10, true);
			SG.setColor(SelecReap_1.sel_color);
			SG.fill3DRect(newHOffset + 320, newvOffset + 50, 10, 10, true);
			SG.setColor(Color.GRAY);
			SG.fill3DRect(newHOffset + 400, newvOffset + 50, 10, 10, true);
		}


		offGraphics.setColor(Color.black);

		offGraphics.draw3DRect(h_offset + base * (pack_width + 7) - 4,v_offset - 3, (window_len) * (pack_width + 7) + 1,pack_height + 6, true);
		offGraphics.draw3DRect(h_offset + receiver_base * (pack_width + 7) - 4,346, ((SelecReap_1.receiver_window_len) * (pack_width + 7) + 1),pack_height + 6, true);
		offGraphics.drawString("Packets Send. " , 4, v_offset - 4);
		offGraphics.drawString("Packets Recev. " , 4, 390);

		for (int i = 0; i < total_Packet; i++){
			offGraphics.setColor(Color.CYAN);
			offGraphics.drawString("" + i, h_offset + (pack_width + 7) * i, v_offset - 4);
			offGraphics.setColor(Color.ORANGE);
			offGraphics.drawString("" + i, h_offset + (pack_width + 7) * i, v_offset + v_clearance + 43);

			if (SenderPack[i] == null) {
				offGraphics.setColor(Color.DARK_GRAY);
				offGraphics.draw3DRect(h_offset + (pack_width + 7) * i,v_offset, pack_width, pack_height, true);
				offGraphics.draw3DRect(h_offset + (pack_width + 7) * i,v_offset + v_clearance, pack_width, pack_height, true);
			} else{
				if (SenderPack[i].acknowledged)
					offGraphics.setColor(SelecReap_1.received_ack);
				else
					offGraphics.setColor(SelecReap_1.unack_color);
				offGraphics.fill3DRect(h_offset + (pack_width + 7) * i,v_offset, pack_width, pack_height, true);
				if (SenderPack[i].buffered)
					offGraphics.setColor(Color.GRAY);
				else

					offGraphics.setColor(SelecReap_1.dest_color);

				if (SenderPack[i].reached_dest)
				{
					offGraphics.fill3DRect(h_offset + (pack_width + 7) * i,v_offset + v_clearance, pack_width, pack_height,true);
				}
				else 
				{
					offGraphics.setColor(Color.black);
					offGraphics.draw3DRect(h_offset + (pack_width + 7) * i,v_offset + v_clearance, pack_width, pack_height,true);
				}

				if (SenderPack[i].on_way)
				{
					if (i == selected)
						offGraphics.setColor(SelecReap_1.sel_color);
					else if (SenderPack[i].Packet_ack)
						offGraphics.setColor(SelecReap_1.roam_pack_color);
					else if (SenderPack[i].received)
						offGraphics.setColor(SelecReap_1.roam_ack_color);
					else
						offGraphics.setColor(SelecReap_1.roam_ack_color);

					if (SenderPack[i].Packet_ack) 
					{
						offGraphics.fill3DRect(h_offset + (pack_width + 7) * i,v_offset + SenderPack[i].Packet_pos, pack_width,pack_height, true);
						offGraphics.setColor(Color.black);
						offGraphics.drawString("" + i, h_offset
								+ (pack_width + 7) * i, v_offset
								+ SenderPack[i].Packet_pos);
					} 
					else
					{
						offGraphics.fill3DRect(h_offset + (pack_width + 7) * i,v_offset + v_clearance - SenderPack[i].Packet_pos,pack_width, pack_height, true);
						if (SenderPack[i].out_of_order) 
						{
							offGraphics.setColor(Color.black);
							offGraphics.drawString("" + SenderPack[i].ackFor,h_offset + (pack_width + 7) * i, v_offset+ v_clearance- SenderPack[i].Packet_pos);
						} 
						else
						{
							offGraphics.setColor(Color.black);
							offGraphics.drawString("" + i, h_offset+(pack_width + 7) * i, v_offset+ v_clearance - SenderPack[i].Packet_pos);
						}
					}
				} 
			} 
		} 
		offGraphics.setColor(Color.pink);
		offGraphics.drawString("BASE = " + base, h_offset + (pack_width + 7)* total_Packet + 10, v_offset + 33);
		offGraphics.drawString("NEXT_SEQUENCE_NUMBER = " + nextseqsum, h_offset+(pack_width + 7) * total_Packet + 10, v_offset + 50);

		offGraphics.setColor(Color.blue);
		offGraphics.drawString("Sender (Send Window Size = " + window_len + ")", h_offset + (pack_width + 7) * total_Packet + 10, v_offset + 12);
		offGraphics.drawString("Receiver (Receiver Window Size = " + SelecReap_1.receiver_window_len + ")", h_offset + (pack_width + 7) * total_Packet + 10, v_offset + v_clearance + 12);


		g.drawImage(offImage, 0, 0, this);
	}


	public boolean onTheWay(SelectiveRepeatPacket pac[]) {
		for (int i = 0; i < pac.length; i++)
		{
			if (pac[i] == null)
				return false;
			else if (pac[i].on_way)
				return true;
		}

		return false;
	}


	public boolean check_upto_n(int packno) 
	{
		for (int i = 0; i < packno; i++)
		{
			if (!SenderPack[i].reached_dest)
				return false;
		}
		return true;
	}


	public void reset_app() {
		for (int i = 0; i < total_Packet; i++){
			if (SenderPack[i] != null)
				SenderPack[i] = null;
		}

		base = 0;
		receiver_base = 0;
		nextseqsum = 0;
		selected = -1;
		fps = SelecReap_1.DEFAULT_FPS;

		timerSleep = false;
		gbnThread = null;



		if (stop.getActionCommand() == "startanim"){
			slow.setEnabled(true);
			fast.setEnabled(true);
		}

		send.setEnabled(true);
		kill.setEnabled(false);
		stop.setLabel("Stop Animation");
		stop.setActionCommand("pause");
		output.append("Simulation restarted. Press 'Send New' to start.\n");
		repaint();
	}


	public class PacketTimerTask extends TimerTask {
		private boolean _cancel = false;
		private int current_index = 0;
		private boolean paused = false;

		public PacketTimerTask(){}

		@Override
		public void run() 
		{  
			int count = 0;
			while(count < SelecReap_1.time_out_sec_def)
			{
				try 
				{
					while(paused)
					{
						Thread.sleep(150);
					}

					Thread.sleep(1200);
				}
				catch (InterruptedException e) 
				{
				}
				count++;
			}

			if(!_cancel)
			{
				RTOP(current_index); 
			}
		}


		public void cancelTimer() 
		{
			_cancel = true;
		}

		public void startTimer() 
		{
			_cancel = false;
		}


		public synchronized void pause() 
		{
			paused = true;
		}


		public synchronized void resume() 
		{
			paused = false;
		}
	}

	class PausableThreadPoolExecutor extends ScheduledThreadPoolExecutor{
		public PausableThreadPoolExecutor(int corePoolSize){
			super(corePoolSize);
		}

		private boolean isPaused;
		private ReentrantLock pauseLock = new ReentrantLock();
		private Condition unpaused = pauseLock.newCondition();

		@Override
		protected void beforeExecute(Thread t, Runnable r) 
		{
			super.beforeExecute(t, r);
			pauseLock.lock();
			try { 			
				while(isPaused) 
					unpaused.await();
			} 
			catch (InterruptedException ie){
				t.interrupt();
			}
			finally 
			{
				pauseLock.unlock();
			}
		}

		public void pause() 
		{
			pauseLock.lock();
			try 
			{
				isPaused = true;

				for(SelectiveRepeatPacket packet : SenderPack)
				{
					if(packet != null)
					{
						if(packet.packet_timer_task != null)
						{
							packet.packet_timer_task.pause();
						}
					}
				}
			}
			finally 
			{
				pauseLock.unlock();
			}
		}


		public void resume() {
			pauseLock.lock();  
			try{
				isPaused = false;	
				unpaused.signalAll();
				for(SelectiveRepeatPacket packet : SenderPack)
				{
					if(packet != null)
					{
						if(packet.packet_timer_task != null)
						{
							packet.packet_timer_task.resume();
						}
					}
				}
			} 
			finally 
			{
				pauseLock.unlock();
			}
		}
	}
} 
