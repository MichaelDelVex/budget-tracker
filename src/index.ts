import { initialiseDatabase } from "./database";
import { seedCategories } from "./categories";
import { seedMerchantRules } from "./merchantResolver";
import { importNabJson } from "./import";

initialiseDatabase();

seedCategories();

seedMerchantRules();

importNabJson(
    "./data/nab-response.json"
);