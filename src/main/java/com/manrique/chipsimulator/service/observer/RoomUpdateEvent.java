package com.manrique.chipsimulator.service.observer;

import com.manrique.chipsimulator.model.Room;

public record RoomUpdateEvent(Room room, String message) {}
