# Distributed System Programming: OCR in the cloud

### Createdby:
    Tom Levy
    Alon Michaeli

### Instructions:

    1. download and Unzip the submitted files / clone the repository .

    2. In the same folder, run from your cmd:
        java -jar LocalApp.jar <input_name> <output_name> <n> ?<terminate>

    3. Program flow:

        * LocalApp:
        3.1 Initiate manager, if needed.
            3.1.1 Create new EC2
            3.1.2 Download ManagerApp.jar, and run it: java -jar ManagerApp.jar
        3.2 Initiate it's unique queue.
        3.3 Listen to it's unique queue for manager's output.
        3.4 Process output, send Termination to manager (if needed), and finish run.

        * ManagerApp:
        3.5 Create manager2workers and workers2manager communication (queues).
        3.6 Main thread listen to locals, executorsPool listen to workers
        3.7 If new task is arrived:
            3.7.1 Send new task to all workers
            3.7.2 update data structures contains data of the mission
            3.7.3
        3.8 If termination is arrived:
            3.8.1 Stop listening to locals (queue).
            3.8.2 Wait until all applications are served (main thread informing the workers-listeners (Thread-pool) by atomic boolean, and .join() until all current tasks are done).
            3.8.3 Finish processing open tasks.
            3.8.4 Send "Terminate" message to all workers, and after all workers respond "worker died", terminate Workers' EC2, and manager EC2.
            3.8.5 finish run.

        * WorkerApp:
        3.9 Listen to messages from manager
        4.0 If a new ocr task arrived, parse text (or error msg if needed) from picture.
        4.1 If "Terminate" message arrived, respond accordingly.
        4.2 finish run.


### Instances specifications, Run and Output (in our final running):

    1. Type of instance:

        1.1 AMI: Ubuntu + Java + aws cli + Tesseract-ocr (tesseract native library).
        1.2 EC2:
            1.2.1 Manager: T2.Small
            1.2.2 Worker: T2.Micro

    2. Run and output:

2.1 Test 1:
        2.1.1 n = 4
        2.1.2 Input size = 24
        2.1.3 Program time until finish working on the input files: 4 minutes.
            2.1.3.1 Additional time until all EC2 terminated: 1 minutes.
            2.1.3.2 Overall: 5 minutes.

        output file attached to zip.

2.2 Test 2 (2 locals):

* first Local:
        2.2.1 n = 12
        2.2 Input size = 24
        2.3 Program time until finish working on the input files: 6 minutes.
            2.3.1 Additional time until all EC2 terminated: 1 minutes.
            2.3.2 Overall: 7 minutes.

* Second Local:
        2.1 n = 5
        2.2 Input size = 24
        2.3 Program time until finish working on the input files: 5 minutes.
            2.3.1 Additional time until all EC2 terminated: 1 minutes.
            2.3.2 Overall: 6 minutes.
        
       output file attached to zip.
	Total time: 7 mins.

### Additional notes & thoughts on the project:

    1. Security:
        1.1 Our credentials are never hard-coded and exposed online.
        1.2 On our local computer, credentials are uploaded in the config file.
        1.3 To launch our EC2 nodes, we used a role with permissions, hence credentials are never exposed.

    2. Scale:
        2.1 Our program implemented for receiving unlimited tasks requests for different clients, since the sqs queue is concurrent.
        2.2 A new task defines a submission of new request to be done (added to data structure, and distributed to workers' handling),
            without any relation previous tasks (executors pool which handle workers responses).
        2.3 Executor pool handle workers responses, hence being able to handle amount of "done ocr tasks" at the same time.
        2.4 Our data structures are concurrent and shared variables are atomic, giving multiple threads the ability to process at the same time.
        2.5 Number of requests from manager is unlimited, but number of listeners to local / workers can be increased, depend on system abilities.

    3. Persistence - errors and recovers:
        3.1 System is programed to handle every worker exception:
            3.1.1 If OCR process fail, an error message is returned
            3.1.2 If worker node crash (or stalls for a while), message is not deleted from queue, so another worker is able to process the task.
        3.2 Manager and Local are able to overcome errors, and everywhere aws operations are conducted, catch{} handle them properly

    4. Threads and concurrency:
        4.1 threads are implemented in manager.
            4.1.1 Used several threads for workers responses, and one for new tasks.
            4.1.2 the executors pool does most of the job (in the manager implementation), and might handle input from a large amount of workers,
            which processed parts of the same request.

    5. System limitations:
        5.1 The system is limited to the instance's type (and capabilities).
        5.2 While worker conduct a simple task and T2.micro might be enough, increasing manager's capabilities can be used to increase number of executors,
            and handle requests faster.

    6. Workers fair work partition:
        6.1 Workers listen to same sqs, and each message polled is Invisible for 60 seconds.
        6.2 We cannot control a round-robin in this situation, but the partition of the work is n urls per user in average.
