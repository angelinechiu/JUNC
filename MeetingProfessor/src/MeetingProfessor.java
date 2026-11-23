import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.Queue;
import java.util.LinkedList;

public class MeetingProfessor {
    public static void main(String[] args) {
        Office office = new Office();
        Professor professor = new Professor(office);
        professor.start();

        // Students come every second
        for (int i = 1; i <= 15; i++) {
            new Student(i, office).start();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            Thread.sleep(25000); // allow remaining consultations to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("End of simulation.");
        System.exit(0);
    }
}

class Office {
    private static final int MAX_SEATS = 2;
    private final Lock lock = new ReentrantLock();
    private final Condition studentsWaiting = lock.newCondition();
    private final Condition turnToConsult = lock.newCondition();

    private Queue<Integer> waitingQueue = new LinkedList<Integer>();
    private boolean professorResting = true;
    private Integer currentStudent = null;

    public void arrive(int studentId) throws InterruptedException {
        lock.lock();
        try {
            if (waitingQueue.size() >= MAX_SEATS) {
                System.out.println("No available seat. Student " + studentId + " just left.");
                return;
            }

            waitingQueue.add(studentId);
            System.out.println("Student " + studentId + " just sat down.");

            if (professorResting) {
                professorResting = false;
                studentsWaiting.signal();
            }

            // Wait until this student is called by the professor
            while (currentStudent == null || currentStudent != studentId) {
                turnToConsult.await();
            }

            // Now consulting
            System.out.println("Student " + studentId + " is consulting.");
        } finally {
            lock.unlock();
        }

        // Consultation happens (professor also sleeping 3 seconds)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Student leaves after consultation
    }

    public void serveNext() throws InterruptedException {
        lock.lock();
        try {
            while (waitingQueue.isEmpty()) {
                professorResting = true;
                studentsWaiting.await();
            }

            professorResting = false;
            int nextId = waitingQueue.poll();
            currentStudent = nextId;
            turnToConsult.signalAll();
        } finally {
            lock.unlock();
        }

        // Explain for 3 seconds
        System.out.println("Professor is explaining.");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // End consultation
        lock.lock();
        try {
            currentStudent = null;
        } finally {
            lock.unlock();
        }
    }
}

class Student extends Thread {
    private int id;
    private Office office;

    public Student(int id, Office office) {
        this.id = id;
        this.office = office;
    }

    @Override
    public void run() {
        try {
            office.arrive(id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class Professor extends Thread {
    private Office office;

    public Professor(Office office) {
        this.office = office;
    }

    @Override
    public void run() {
        try {
            while (true) {
                office.serveNext();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
