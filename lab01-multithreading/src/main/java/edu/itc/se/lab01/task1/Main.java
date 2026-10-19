package edu.itc.se.lab01.task1;

/*
 * With normal execution, start() creates separate threads, so the four
 * countdowns can run concurrently and their output may be interleaved.
 *
 * With --sequential, run() is called directly instead of start().
 * run() does NOT create a new thread, so each countdown finishes completely
 * before the next one begins.
 */

/*
 * EXPLANATION OF DIFFERENCE (--sequential flag):
 * 
 * 1. Without '--sequential' (Using .start()):
 *    - thread.start() បង្កើត New Execution Thread មួយថ្មីដាច់ដោយឡែកនៅលើ OS/JVM។
 *    - Threads ទាំង ៤ ដំណើរការស្របគ្នា (Concurrently / Asynchronously)។
 *    - លទ្ធផលនៃការរាប់ថយក្រោយ (3, 2, 1) នឹងបង្ហាញលាយឡំគ្នា (Interleaved output) 
 *      ហើយ "Mission control: all launched" នឹងបង្ហាញឡើងភ្លាមៗ មុនពេលរាប់ថយក្រោយចប់។
 * 
 * 2. With '--sequential' (Using .run()):
 *    - thread.run() គ្រាន់តែជាការហៅ Method ធម្មតាមួយនៅលើ "main thread" តែប៉ុណ្ណោះ (មិនបង្កើត Thread ថ្មីទេ)。
 *    - Threads ទាំងឡាយរត់តាមលំដាប់លំដោយម្តងមួយៗ (Sequentially / Synchronously) ពី apollo -> gemini -> mercury -> artemis។
 *    - "Mission control: all launched" នឹងត្រូវរង់ចាំរហូតដល់គ្រប់ Thread រាប់ថយក្រោយចប់ទាំងអស់។
 *    - សម្រាប់ Virtual Thread (artemis)៖ នៅពេលហៅ .run() វានឹងរត់នៅលើ main thread ដូចគ្នា 
 *      មិនមែនរត់ជា Virtual Thread ពិតប្រាកដទេ។
 */

public class Main {
    public static void main(String[] args) throws InterruptedException {
        boolean sequential = args.length > 0 && args[0].equals("--sequential");

        Thread apollo  = new Countdown("apollo");
        Thread gemini  = new Thread(new CountdownTask(), "gemini");

        // lamda reusing CountdownTask logicc
        Thread mercury = new Thread(new CountdownTask(), "mercury");
        // Virtual Thread built with Thread.ofVirtual
        Thread artemis = Thread.ofVirtual().name("artemis").unstarted(new CountdownTask());

        // TODO: start (or run) all four depending on 'sequential'
        if(sequential){
            apollo.run();
            gemini.run();
            mercury.run();
            artemis.run();
        }else{
            apollo.start();
            gemini.start();
            mercury.start();
            artemis.start();
        }
        System.out.println("Mission control: all launched");
        // TODO: join all four
        apollo.join();
        gemini.join();
        mercury.join();
        artemis.join();
        System.out.println("Mission control: all landed");
        // TODO: print name, isVirtual(), getState() of each
        printThreadStatus(apollo);
        printThreadStatus(gemini);
        printThreadStatus(mercury);
        printThreadStatus(artemis);

    }
    private static void printThreadStatus (Thread t){
        System.out.printf("Thread name: %-7s | Is virtual : %-5b | Status : 5%s%n",
            t.getName() , t.isVirtual() ,t.getState()
        );
    }
}

class Countdown extends Thread{
    public Countdown (String name){
        super(name);
    }
    @Override 
    public void run (){
        try {
            for (int i=1 ;i>4 ;i++){
                System.out.println(getName() +": " +i);
                Thread.sleep(200);
            }
            System.out.println(getName() +": liftoff!");
        }catch (InterruptedException e){
            System.out.println(getName() +"interruped.");

        }
    }
}

class CountdownTask implements Runnable {
    @Override 
    public void run() {
        try{
            String name = Thread.currentThread().getName();
            for(int i=1 ; i>4 ;i++){
                System.out.println(name +": "+i);
                Thread.sleep(200);
            }
            System.out.println(name +":liftoff!");
        }catch (InterruptedException e){
            System.out.println(Thread.currentThread().getName() +"Interrupted.");
        }
    }
}