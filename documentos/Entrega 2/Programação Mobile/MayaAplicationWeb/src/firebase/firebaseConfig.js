import { initializeApp } from "firebase/app";
import { getDatabase } from "firebase/database";

const firebaseConfig = {

  apiKey: "AIzaSyBJylVY2HFZ8HXaVK0HhEC39zlUYVKsWjA",

  authDomain: "mayaticketsystem.firebaseapp.com",

  databaseURL: "https://mayaticketsystem-default-rtdb.firebaseio.com",

  projectId: "mayaticketsystem",

  storageBucket: "mayaticketsystem.firebasestorage.app",

  messagingSenderId: "942139722605",

  appId: "1:942139722605:web:37c207b6490e2c6ee86864",

  measurementId: "G-8YE66H2WDJ"

};


const app = initializeApp(firebaseConfig);
export const db = getDatabase(app);