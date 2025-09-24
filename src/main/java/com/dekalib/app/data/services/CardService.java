package com.dekalib.app.data.services;

import com.dekalib.app.data.entities.Card;
import com.dekalib.app.data.entities.YooKassa;
import com.dekalib.app.data.repositories.CardRepository;
import com.dekalib.app.data.repositories.YooKassaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CardService {
    private final CardRepository cardRepository;
    private final YooKassaRepository yooKassaRepository;
    @Autowired
    public CardService(CardRepository cardRepository,
                       YooKassaRepository yooKassaRepository) {
        this.cardRepository = cardRepository;
        this.yooKassaRepository = yooKassaRepository;
    }
    public String getCard() {

        List<Card> cards = (List<Card>) cardRepository.findAll();
        if (!cards.isEmpty()) {
            return cards.get(0).getId() + "  " + cards.get(0).getBank();
        }
        return null;
    }

    public YooKassa getKassa() {
        List<YooKassa> yooKassa = (List<YooKassa>) yooKassaRepository.findAll();
        if (!yooKassa.isEmpty()) {
            return yooKassa.get(0);
        }
        return null;
    }

    public Card getCountryCard(String code) {
        List<Card> cards = (List<Card>) cardRepository.findAll();
        for(Card card : cards) {
            if (card.getCountry() == code) return card;

        }
        return null;
    }

}
