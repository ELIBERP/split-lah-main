# Split-Lah

## The Singaporean Way to Split Bills

Split-Lah is a mobile application designed to make bill splitting effortless, inspired by Singapore's unique "lah" expression. Our app simplifies expense tracking and debt settlement among friends, families, and groups.

## 📱 Features

- **Group Management**: Create permanent groups for recurring expenses with friends, family, or colleagues
- **Multi-Currency Support**: Handle expenses in different currencies
- **Smart Debt Simplification**: Optimized debt paths to minimize the number of transactions needed to settle debts
- **Real-time Updates**: Instant notifications when transactions are recorded or debts are settled
- **Expense Categories**: Categorize expenses (food, entertainment, transport, etc.) for better tracking
- **Expense History**: View transaction history with detailed information
- **User Profiles**: Personalized profiles with customizable icons

## 🎨 Project Poster

Our project poster provides an overview of Split-Lah's key features and design philosophy:

![Split-Lah Project Poster](/photos/A2%20info%20sys%20poster.png)

## 📹 Project Video

Our project video provides an overview idea of Split-Lah's key features:

![Project Video](https://youtube.com/shorts/OcC1ra79u8Y?feature=share)

## 🖼️ Screenshots

![Home Screen](/photos/Overview.png)
![Records](/photos/Records.png)
![Groups](/photos/Groups.png)
![Bill Splitting](/photos/BillSplitting.png)
![Debt Relations](/photos/DebtRelations.png)
![Settings](/photos/Settings.png)

## 🎨 Figma Prototype

Before development, we created a comprehensive prototype to refine our UI/UX design:

View our [complete Figma design file](https://www.figma.com/design/Xh5J1n3g4jA5sOfBbZz8Pt/ISP-Mobile-App?node-id=797-768&t=z1wpZPJicmAwqpnr-1) to see our design process, component library, and user flow mapping.

Key design principles:
- Clean, intuitive interface prioritizing quick bill entry
- Clear visualization of debts and settlements
- Consistent visual language with Singapore-inspired elements
- Accessibility considerations for diverse users

## 🔧 Technologies Used

- **Frontend**: Java, Android SDK
- **Backend**: Firebase Firestore
- **Authentication**: Firebase Authentication
- **UI Components**: Material Design
- **Real-time Communication**: Firebase Cloud Messaging

## 📋 Installation

### Prerequisites
- Android Studio
- Firebase account
- Java Development Kit (JDK) 11 or higher

### Setup
1. Clone the repository
`git clone https://github.com/your-username/split-lah.git `
2. Open the project in Android Studio
3. Connect your Firebase project:
- Add `google-services.json` to the app directory
- Enable Authentication and Firestore in Firebase Console
4. Build and run the app on an emulator or physical device

## 🧩 Project Structure
```
app/ 
├── src/ 
│ ├── main/ 
│ │ ├── java/com/example/split_lah/ 
│ │ │ ├── models/ # Data models 
│ │ │ │ ├── TransactionLine.java 
│ │ │ │ ├── User.java 
│ │ │ │ └── IconUtils.java 
│ │ │ ├── ui/ # UI components 
│ │ │ │ ├── home/ # Home screen components 
│ │ │ │ ├── split/ # Bill splitting functionality 
│ │ │ │ ├── debt_relation/ # Debt relations management 
│ │ │ │ ├── records/ # Transaction records 
│ │ │ │ ├── net_balances/ # Net balances visualization 
│ │ │ │ └── members/ # Group members management 
│ │ │ ├── shared_view_model/ # Shared data between fragments 
│ │ │ ├── utils/ # Utility classes 
│ │ │ └── MainActivity.java # Main activity & navigation 
│ │ ├── res/ # Resources 
│ │ │ ├── layout/ # UI layouts 
│ │ │ ├── drawable/ # Icons and images 
│ │ │ ├── values/ # Strings, colors, styles 
│ │ │ └── navigation/ # Navigation graphs 
│ │ └── AndroidManifest.xml # App configuration 
│ └── test/ # Unit tests 
├── build.gradle # Project build configuration 
└── google-services.json # Firebase configuration
```

The structure shows how we've organized the app into logical components:
- Separated UI by feature (home, split, debt relations, etc.)
- Clear model definitions for data handling
- Shared ViewModel architecture for state management
- Resource organization following Android best practices

## 💡 Future Enhancements

- [ ] Implement OCR for receipt scanning
- [ ] Add integration with payment systems
- [ ] Develop iOS version
- [ ] Enable expense analytics and reporting
- [ ] Add support for recurring expenses

## 👥 Contributors

- [Elizabeth](https://github.com/ELIBERP)
- [Zhi Xun](https://github.com/zed-ex)
- [Janelle](https://github.com/janfjxuan)
- [Jana](https://github.com/janaleong)
- [Natasha](https://github.com/natasha-sutd)
- [Ky](https://github.com/Kydinhvan)
- [Freddie](https://github.com/FredSterz)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by the Singaporean culture and "lah" expression
- Thanks to SUTD for supporting this project
- Special thanks to [Mithun Mohan K](https://medium.com/@mithunmk93) for his article [Algorithm Behind Splitwise's Debt Simplification Feature](https://medium.com/@mithunmk93/algorithm-behind-splitwises-debt-simplification-feature-8ac485e97688), which we use for our debt simplification implementation
- The debt simplification algorithm in our app is based on Mithun's work on simplifying IOUs between multiple people
- Additional thanks to:
  - Firebase for their excellent backend services
  - Material Design for UI guidelines
  - Android development community for their valuable resources