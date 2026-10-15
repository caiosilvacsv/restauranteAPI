package com.restaurant.api.domain.enums;

import com.restaurant.api.domain.exception.InvalidStateException;

import java.util.Set;

public enum StatusOrderItem {
  PENDING,
  QUEUED,
  PREPARING,
  READY,
  DESPATCHED,
  DELIVERED,
  CLOSED,
  CANCELLED;

  public boolean canTransitionTo(StatusOrderItem nextStatus){
    return switch (this){
      case PENDING ->
          nextStatus == PREPARING || nextStatus == QUEUED || nextStatus == CANCELLED;
      case QUEUED ->
          nextStatus == PREPARING || nextStatus == READY || nextStatus == CANCELLED;
      case PREPARING ->
          nextStatus == READY || nextStatus == CANCELLED;
      case READY ->
          nextStatus == DESPATCHED || nextStatus == DELIVERED || nextStatus == CANCELLED;
      case DESPATCHED ->
          nextStatus == DELIVERED || nextStatus == CLOSED || nextStatus == CANCELLED;
      case DELIVERED ->
          nextStatus == CLOSED || nextStatus ==CANCELLED;
      case CLOSED, CANCELLED ->  false;
    };
  }

  public StatusOrderItem next(){
    return switch (this){
      case PENDING -> QUEUED;
      case QUEUED -> PREPARING;
      case PREPARING -> READY;
      case READY -> DESPATCHED;
      case DESPATCHED -> DELIVERED;
      case DELIVERED -> CLOSED;
      case CLOSED,  CANCELLED -> throw new InvalidStateException(
          "Invalid Operation",
          "Cannot advance order from a final status: " + this
      ) ;
    };
  }

  public boolean isInKitchen(){
    return this == PREPARING || this == READY || this == QUEUED;
  }

  public boolean isInDelivery(){
    return this == DESPATCHED || this == DELIVERED;
  }

  public boolean isFinish(){
    return this == CLOSED || this == CANCELLED;
  }
}
