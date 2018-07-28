#pragma once

class MoneyManager
{
public:
    MoneyManager();

    void addMoney(double cash);
    double getAmount();
private:
    double amount;
};
