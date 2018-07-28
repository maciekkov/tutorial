#include "haxer/moneyManagement/MoneyManager.hpp"

MoneyManager::MoneyManager()
{
}

void MoneyManager::addMoney(double cash)
{
    amount += cash;
}

double MoneyManager::getAmount()
{
    return amount;
}
