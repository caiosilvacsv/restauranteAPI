package com.restaurant.api.domain.enums;
import com.restaurant.api.domain.exception.InvalidStateException;

public enum StatusOrder {
  OPENED,
  PREPARING,
  READY,
  DESPATCHED,
  DELIVERED,
  CLOSED,
  CANCELLED;

  /**
   * Next status in order flow;
   *
   * @return {@code StatusOrder} The next order in flow
   * @throws InvalidStateException invalid alter in status closed or canceled;
   */
  public StatusOrder next (){
    return switch (this){
      case OPENED -> PREPARING;
      case PREPARING -> READY;
      case READY -> DESPATCHED;
      case DESPATCHED -> DELIVERED;
      case DELIVERED -> CLOSED;
      case CLOSED,CANCELLED -> throw new InvalidStateException(
          "Invalid operation",
          "Cannot advance order from a final status: " + this
      );

    };
  }

  /**
   * The order return the next status possible in order flow;
   *
   * @param nextStatus reference status to be validated.
   * @return {@code true} case is valid next status;
   *         {@code false} if is closed or canceled
   */

  public boolean canTransitionTo(StatusOrder nextStatus){
    if (nextStatus == CLOSED) {
      return this != CLOSED && this != CANCELLED;
    }
    return switch (this){
      case OPENED -> nextStatus == PREPARING || nextStatus == CANCELLED;
      case PREPARING -> nextStatus == READY || nextStatus == CANCELLED;
      case READY -> nextStatus == DESPATCHED || nextStatus == DELIVERED || nextStatus == CANCELLED;
      case DESPATCHED -> nextStatus == DELIVERED || nextStatus == CANCELLED;
      case DELIVERED -> true;
      case CLOSED, CANCELLED -> false;
    };
  }

  /**
   * Order in closed or canceled
   *
   * @return {@code true} is closed or canceled
   */
  public boolean isFinished(){
    return this == CLOSED || this == CANCELLED;
  }

  /**
   *  Order in kitchen.
   *
   * @return {@code true} is preparing or ready
   */
  public boolean isKitchen(){
    return this == PREPARING || this == READY;
  }

  /**
   * The order is open for additional items to be added.
   *
   * @return {@code true} If in a state where items can be added.
   *        {@code OPENED, PREPARING, READY}
   */
  public boolean isOpen(){
    return this == OPENED || this == PREPARING || this ==READY;
  }

}
