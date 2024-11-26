import { createBrowserRouter } from 'react-router-dom'
import GeneralError from './pages/errors/general-error'
import NotFoundError from './pages/errors/not-found-error'

const router = createBrowserRouter([
  // Auth routes
  {
    path: '/sign-in-2',
    lazy: async () => ({
      Component: (await import('./pages/auth/sign-in-2')).default,
    }),
  },

  // Main routes
  {
    path: '/',
    lazy: async () => {
      const AppShell = await import('./components/app-shell')
      return { Component: AppShell.default }
    },
    errorElement: <GeneralError />,
    children: [
      {
        index: true,
        lazy: async () => ({
          Component: (await import('./pages/home')).default,
        }),
      },
      {
        path: 'tasks',
        lazy: async () => ({
          Component: (await import('@/pages/tasks')).default,
        }),
      },
      // {
      //   path: 'lesson',
      //   lazy: async () => ({
      //     Component: (await import('@/pages/simpleList/LessonView')),
      //   }),
      // },
      {
        path: 'list',
        lazy: async () => ({
          Component: (await import('@/pages/simpleList')).default,
        }),
      },
    ],
  },

  // Error routes
  { path: '/500', Component: GeneralError },
  { path: '/404', Component: NotFoundError },
  // Fallback 404 route
  { path: '*', Component: NotFoundError },
])

export default router
