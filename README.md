# selective-repeat-protocol

Developed a selective repeat protocol in Java.

About Testing the Program:

1) Opening the entire folder in Eclipse 
2) Run the file "SelectiveRepeat.java"


This protocol is mostly identical to GBN protocol, except that buffers are used and the receiver, and the sender, each maintain a window of size .

Suppose the sequence number of frame is generated modulo , that is, they are 0, 1, 2, ..., , 0, 1, 2,... then we must have . 
->The sender and receiver each maintain a buffer of its own window size;
When there is an error, the receiver freezes the lower edge to the last sequence number before the lost frame sequence number;
As long as the window size is less than , the receiver continues to receive and acknowledge incoming frames;
The sender maintains a timeout clock for each of the unacknowledged frame number and retransmit that frame after timeout.
Acknowledgement will be piggybacked to the sender. But when there is no traffic in the reverse direction, piggyback is impossible, a special timer will time out for the ACK, so that the ACK is sent back as an independent packet. If the receiver suspects that the transmission has error, it immediately fires back a negative acknowledgement (NAK) to the sender.
SRP works better when the link is very unreliable. Because in this case, retransmission tends to happen more frequently, selectively retransmitting frames is more efficient than retransmitting all of them. SRP also requires full duplex link. backward acknowledgements are also in progress.


Description taken from this site: https://www.rpi.edu/locker/75/000475/main/subsubsection3_8_2_3.html

I have made this project as part my coursework CS 420 - Computer Networks
