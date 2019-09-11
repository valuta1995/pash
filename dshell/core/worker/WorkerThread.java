package dshell.core.worker;

import dshell.core.Operator;
import dshell.core.OperatorFactory;
import dshell.core.nodes.Sink;

import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class WorkerThread extends Thread {
    private Socket client;

    public WorkerThread(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try (Socket socket = this.client;
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            RemoteExecutionData red = (RemoteExecutionData) inputStream.readObject();
            Operator operator = red.getOperator();

            if (!(red.getOperator() instanceof Sink)) {
                operator.subscribe(OperatorFactory.createSocketedOutput(red.getOutputHost(), red.getOutputPort()));
                operator.next(0, red);
            } else // instance of sink
            {
                operator.next(0, red.getInputData());

                // let know the client that the system has finished computing
                try (Socket callbackSocket = new Socket(red.getCallbackHost(), red.getCallBackPort());
                     ObjectOutputStream callbackOOS = new ObjectOutputStream(callbackSocket.getOutputStream())) {

                    callbackOOS.writeBoolean(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}