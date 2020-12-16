# Distributed System Programming: OCR in the cloud

* Developed a distributed system, using AWS Java SDK. 
* The system Analyzes text from pictures (using OCR library) received
from clients, and capable to perform on a large scale of
requests. 
* Using AWS tools such as:
	i.	EC2 - AWS Elastic Computing
	ii.	S3 – Cloud storage
	iii.	Amazon SQS - Message Queuing Service


#### Instructions:

    1. download and Unzip the submitted files / clone the repository .

    2. In the same folder, run from your cmd:
        java -jar LocalApp.jar <input_name> <output_name> <n> ?<terminate>

#### Program flow:

        * LocalApp:
        1.1 Initiate manager, if needed.
            1.1.1 Create new EC2
            1.1.2 Download ManagerApp.jar, and run it: java -jar ManagerApp.jar
        1.2 Initiate it's unique queue.
        1.3 Listen to it's unique queue for manager's output.
        1.4 Process output, send Termination to manager (if needed), and finish run.

        * ManagerApp:
        2.1 Create manager2workers and workers2manager communication (queues).
        2.2 Main thread listen to locals, executorsPool listen to workers
        2.3 If new task is arrived:
            2.3.1 Send new task to all workers
            2.3.2 update data structures contains data of the mission
            2.3.3
        2.8 If termination is arrived:
            2.3.1 Stop listening to locals (queue).
            2.3.2 Wait until all applications are served (main thread informing the workers-listeners (Thread-pool) by atomic boolean, and .join() until all current tasks are done).
            2.3.3 Finish processing open tasks.
            2.3.4 Send "Terminate" message to all workers, and after all workers respond "worker died", terminate Workers' EC2, and manager EC2.
            2.3.5 finish run.

        * WorkerApp:
        3.1 Listen to messages from manager
        3.2 If a new ocr task arrived, parse text (or error msg if needed) from picture.
        3.3 If "Terminate" message arrived, respond accordingly.
        3.4 finish run.

