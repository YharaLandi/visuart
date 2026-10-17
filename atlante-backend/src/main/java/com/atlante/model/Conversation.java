package com.atlante.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Chat 1-a-1. La coppia è ordinata per username, così (a,b) e (b,a) sono la stessa riga. */
@Entity
@Table(name = "conversations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_one_id", "user_two_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_one_id")
    private User userOne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_two_id")
    private User userTwo;

    private Conversation(User userOne, User userTwo) {
        this.userOne = userOne;
        this.userTwo = userTwo;
    }

    public static Conversation between(User a, User b) {
        return new Conversation(first(a, b), second(a, b));
    }

    public static User first(User a, User b) {
        return a.getUsername().compareTo(b.getUsername()) <= 0 ? a : b;
    }

    public static User second(User a, User b) {
        return a.getUsername().compareTo(b.getUsername()) <= 0 ? b : a;
    }

    public User counterpartOf(String username) {
        return userOne.getUsername().equals(username) ? userTwo : userOne;
    }

    public boolean includes(String username) {
        return userOne.getUsername().equals(username) || userTwo.getUsername().equals(username);
    }
}
