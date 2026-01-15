package com.example.accesscontrol.model;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Stats {
    private final AtomicInteger eventosRecebidos = new AtomicInteger(0);
    private final AtomicInteger disparosRealizados = new AtomicInteger(0);
    
    public void incrementarEventos() {
        eventosRecebidos.incrementAndGet();
    }
    
    public void incrementarDisparos() {
        disparosRealizados.incrementAndGet();
    }
    
    // NOVO MÉTODO: Reset dos contadores
    public void reset() {
        eventosRecebidos.set(0);
        disparosRealizados.set(0);
    }
    
    public int getEventosRecebidos() {
        return eventosRecebidos.get();
    }
    
    public int getDisparosRealizados() {
        return disparosRealizados.get();
    }
}