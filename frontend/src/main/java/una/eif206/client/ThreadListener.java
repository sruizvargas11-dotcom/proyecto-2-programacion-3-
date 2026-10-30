package una.eif206.client;

import una.eif206.model.Reserva;

public interface ThreadListener {

    /** Llego una reserva que creo OTRO usuario. */
    void deliver_message(Reserva reserva);
}