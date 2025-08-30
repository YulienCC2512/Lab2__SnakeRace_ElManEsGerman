/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.primefinder;

import java.util.Scanner;
/**
 *
 */
public class Control extends Thread {
    
    private final static int NTHREADS = 3;
    private final static int MAXVALUE = 30000000;
    private final static int TMILISECONDS = 5000;

    private final int NDATA = MAXVALUE / NTHREADS;

    private PrimeFinderThread pft[];
    private final PauseController controller = new PauseController();
    
    private Control() {
        super();
        this.pft = new  PrimeFinderThread[NTHREADS];

        int i;
        for(i = 0;i < NTHREADS - 1; i++) {
            PrimeFinderThread elem = new PrimeFinderThread(i*NDATA, (i+1)*NDATA, controller);
            pft[i] = elem;
        }
        pft[i] = new PrimeFinderThread(i*NDATA, MAXVALUE + 1, controller);
    }
    
    public static Control newControl() {
        return new Control();
    }

    @Override
    public void run() {
        for(int i = 0;i < NTHREADS;i++ ) {
            pft[i].start();
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                Thread.sleep(TMILISECONDS);
                //pause
                controller.pauseAll();
                System.out.println("....Paused....");
                //primes in the moment
                int count = 0;
                for(PrimeFinderThread t : pft) {
                    count += t.getPrimes().size();
                }
                System.out.println("Primes found: " + count);
                //wait for user to resume
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                //resume
                controller.resumeAll();
                System.out.println("....Resumed....");
            }
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
}
