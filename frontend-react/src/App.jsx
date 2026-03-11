import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { TemplateProvider } from './context/TemplateContext';
import MainPortal from './components/MainPortal';
import HtmlEditorPage from './components/HtmlEditorPage';
import './App.css';

function App() {
  return (
    <Router>
      <TemplateProvider>
        <Routes>
          <Route path="/" element={<MainPortal />} />
          <Route path="/editor" element={<HtmlEditorPage />} />
        </Routes>
      </TemplateProvider>
    </Router>
  );
}

export default App;
