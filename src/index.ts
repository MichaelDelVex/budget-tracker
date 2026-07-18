import { initialiseDatabase } from "./database";
import { importNabJson } from "./import";

console.log("Starting import");

initialiseDatabase();

importNabJson(
    "./data/nab-response.json"
);

console.log("Import complete");