import { ReactNode } from 'react'

export const metadata = {
  title: 'Sovereign Rise API',
  description: 'Backend API for Sovereign Rise mobile app',
}

export default function RootLayout({
  children,
}: {
  children: ReactNode
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
